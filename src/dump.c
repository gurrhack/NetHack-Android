/*	SCCS Id: @(#)end.c	3.5	2010/03/08	*/
/* Copyright (c) Patric Mueller.			*/
/* NetHack may be freely redistributed.  See license for details. */

#include "hack.h"
#include "date.h"

#ifdef DUMP_LOG

# ifdef HAVE_UTIME_H
#  include <utime.h>
#  include <errno.h>
# endif
#ifdef ANDROID
#  include <sys/stat.h>
#endif
extern char msgs[][BUFSZ];
extern int lastmsg;
void FDECL(do_vanquished, (int, BOOLEAN_P));
#endif 

#ifdef DUMP_LOG
FILE *dump_fp = (FILE *)0;  /**< file pointer for text dumps */
char dump_path[BUFSIZ];
FILE *html_dump_fp = (FILE *)0;  /**< file pointer for html dumps */
char html_dump_path[BUFSIZ];
/* TODO:
 * - escape unmasked characters in html (done for map)
 * - started/ended date at the top
 */

static int count_fmt(char* format, int ch) {
	int count = 0;
	while((format = index(format, '%')))
		if(*(format++)==ch) count++;
	return count;
}

static
char*
get_dump_filename()
{
	int nn = count_fmt(dump_fn, 'n') + count_fmt(dump_fn, 's');
	int nd = count_fmt(dump_fn, 'd');
	int plname_len = strlen(plname);
	int date_len = 15;
	int new_dump_fn_len = strlen(dump_fn) + nn*plname_len + nd*date_len + 5; /* space for ".html" */;
	char *new_dump_fn = (char *) alloc((unsigned)(new_dump_fn_len+1));
	char *dst = new_dump_fn;
	char *ps = dump_fn, *pe;
	time_t now = getnow();

	while((pe = index(ps, '%'))) {
		memcpy(dst, ps, pe-ps);
		dst += pe-ps;
		if(pe[1]=='n' || pe[1]=='s') {
			strcpy(dst, plname);
			regularize(dst);
			dst += plname_len;
		} else if(pe[1]=='d') {
			sprintf(dst, "%ld_%ld", yyyymmdd(now), hhmmss(now));
			dst += date_len;
		} else {
			*dst++ = '%';
			pe--;
		}
		ps = pe + 2;
	}
	strcpy(dst, ps);
	return new_dump_fn;
}

static void
mkdir_p(filename)
char* filename;
{
	char *de, *ds = filename;
	if(*ds == '/') ds++;
	while((de = index(ds, '/'))) {
		*de = 0;
		mkdir(filename, 0777);
		*de = '/';
		ds = de + 1;
	}
}
void
dump_init()
{
  if (dump_fn[0]) {
    char *new_dump_fn = get_dump_filename();
    if(!new_dump_fn)
    	return;

#ifdef ANDROID
	mkdir_p(new_dump_fn);
#endif

if(dump_format&DUMP_FORMAT_TEXT) {
#ifdef ANDROID
    strncpy(dump_path, strcat(new_dump_fn, ".txt"), BUFSIZ-1);
#else
    strncpy(dump_path, new_dump_fn, BUFSIZ-1);
#endif
    dump_fp = fopen(new_dump_fn, "w");
    if (!dump_fp) {
	pline("Can't open %s for output.", new_dump_fn);
	pline("Dump file not created.");
    }
#ifdef ANDROID
	new_dump_fn[strlen(new_dump_fn)-4] = 0;
#endif
}
if(dump_format&DUMP_FORMAT_HTML) {
    strncpy(html_dump_path, strcat(new_dump_fn, ".html"), BUFSIZ-1);
    html_dump_fp = fopen(html_dump_path, "w");
    if (!html_dump_fp) {
	pline("Can't open %s for output.", new_dump_fn);
	pline("Html dump file not created.");
    }
}
    if (new_dump_fn) free(new_dump_fn);
  }
}
#endif

#ifdef DUMP_LOG
/** Set a file's access and modify time to u.udeathday. */
static void
adjust_file_timestamp(fpath)
const char* fpath;
{
# ifdef HAVE_UTIME_H
	if (u.udeathday > 0) {
		struct utimbuf tv;
		tv.actime = u.udeathday;
		tv.modtime = u.udeathday;
		if (utime(fpath, &tv)) {
			paniclog("adjust_file_timestamp: utime failed: ", strerror(errno));
		}
	}
# endif
}
#endif

void
dump_exit()
{
#ifdef DUMP_LOG
	if (dump_fp) {
		fclose(dump_fp);
		dump_fp = NULL;
		adjust_file_timestamp(dump_path);
	}
	if (html_dump_fp) {
		dump_html("</body>\n</html>\n","");
		fclose(html_dump_fp);
		html_dump_fp = NULL;
		adjust_file_timestamp(html_dump_path);
	}
#endif
}

void
dump(pre, str)
const char *pre, *str;
{
#ifdef DUMP_LOG
  if (dump_fp)
    fprintf(dump_fp, "%s%s\n", pre, str);
  if (html_dump_fp)
    fprintf(html_dump_fp, "%s%s\n", pre, str);
#endif
}

/** Outputs a string only into the html dump. */
void
dump_html(format, str)
const char *format, *str;
{
#ifdef DUMP_LOG
  if (html_dump_fp)
    fprintf(html_dump_fp, format, str);
#endif
}

/** Outputs a string only into the text dump. */
void
dump_text(format, str)
const char *format, *str;
{
#ifdef DUMP_LOG
  if (dump_fp)
    fprintf(dump_fp, format, str);
#endif
}

/** Dumps one line as is. */
void
dump_line(pre, str)
const char *pre, *str;
{
#ifdef DUMP_LOG
  if (dump_fp)
    fprintf(dump_fp, "%s%s\n", pre, str);
  if (html_dump_fp)
    fprintf(html_dump_fp, "%s%s<br />\n", pre, str);
#endif
}

static char tmp_html_link[BUFSZ];
/** Return a link to nethackwiki . */
char *
html_link(link_name, name)
const char *link_name;
const char *name;
{
	snprintf(tmp_html_link, BUFSZ,
		"<a href=\"http://nethackwiki.com/wiki/%s\">%s</a>",
		link_name, name);
	return tmp_html_link;
}

/** Dumps an object from the inventory. */
void
dump_object(c, obj, str)
const char c;
const struct obj *obj;
const char *str;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %c - %s\n", c, str);
	if (html_dump_fp) {
		char *link = html_link(dump_typename(obj->otyp), str);
# if defined(TTY_GRAPHICS) || defined(ANDROID_GRAPHICS)
		int color;
		int attr;
		if (iflags.use_menu_color &&
		    get_menu_coloring((char*)str, &color, &attr)) {
			fprintf(html_dump_fp, "<span class=\"nh_color_%d\"><span class=\"nh_item_letter\">%c</span> - %s</span><br />\n", color, c, link);
		} else
# endif
		fprintf(html_dump_fp, "<span class=\"nh_item_letter\">%c</span> - %s<br />\n", c, link);
	}
#endif
}

/** Dumps a secondary title. */
void
dump_subtitle(str)
const char *str;
{
#ifdef DUMP_LOG
  dump_text("  %s\n", str);
  dump_html("<h3>%s</h3>\n", str);
#endif
}

/** Dump a title. Strips : from the end of str. */
void
dump_title(str)
char *str;
{
#ifdef DUMP_LOG
	int len = strlen(str);
	if (str[len-1] == ':') {
		str[len-1] = '\0';
	}
	if (dump_fp)
		fprintf(dump_fp, "%s\n", str);
	if (html_dump_fp) {
		fprintf(html_dump_fp, "<h2>%s</h2>\n", str);
		}
#endif
}

/** Starts a list in the dump. */
void
dump_list_start()
{
#ifdef DUMP_LOG
	if (html_dump_fp)
		fprintf(html_dump_fp, "<ul>\n");
#endif
}

/** Dumps a linked list item. */
void
dump_list_item_link(link, str)
const char *link;
const char *str;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %s\n", str);
	if (html_dump_fp)
		fprintf(html_dump_fp, "<li>%s</li>\n", html_link(link, str));
#endif
}

/** Dumps an object as list item. */
void
dump_list_item_object(obj)
struct obj *obj;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %s\n", doname(obj));
	if (html_dump_fp) {
		const char* str = doname(obj);
		char *link = html_link(dump_typename(obj->otyp), str);
# if defined(TTY_GRAPHICS) || defined(ANDROID_GRAPHICS)
		int color;
		int attr;
		if (iflags.use_menu_color &&
		    get_menu_coloring((char*)str, &color, &attr)) {
			fprintf(html_dump_fp, "<li class=\"nh_color_%d\">%s</li>\n", color, link);
		} else
#endif
		fprintf(html_dump_fp, "<li>%s</li>\n", link);
	}
#endif
}

/** Dumps a list item. */
void
dump_list_item(str)
const char *str;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %s\n", str);
	if (html_dump_fp)
		fprintf(html_dump_fp, "<li>%s</li>\n", str);
#endif
}

/** Ends a list in the dump. */
void
dump_list_end()
{
	dump_html("</ul>\n","");
}

/** Starts a blockquote in the dump. */
void
dump_blockquote_start()
{
#ifdef DUMP_LOG
	if (html_dump_fp)
		fprintf(html_dump_fp, "<blockquote>\n");
#endif
}

/** Ends a blockquote in the dump. */
void
dump_blockquote_end()
{
#ifdef DUMP_LOG
	dump_text("\n", "");
	dump_html("</blockquote>\n", "");
#endif
}

/** Starts a definition list in the dump. */
void
dump_definition_list_start()
{
#ifdef DUMP_LOG
	if (html_dump_fp)
		fprintf(html_dump_fp, "<dl>\n");
#endif
}

/** Dumps a definition list item. */
void
dump_definition_list_dt(str)
const char *str;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %s\n", str);
	if (html_dump_fp) {
		fprintf(html_dump_fp, "<dt>");
		while (*str != '\0')
			fprintf(html_dump_fp, "%s", html_escape_character(*str++));
		fprintf(html_dump_fp, "</dt>\n");
	}
#endif
}

/** Dumps a definition list item. */
void
dump_definition_list_dd(str)
const char *str;
{
#ifdef DUMP_LOG
	if (dump_fp)
		fprintf(dump_fp, "  %s\n", str);
	if (html_dump_fp)
		fprintf(html_dump_fp, "<dd>%s</dd>\n", str);
#endif
}

/** Ends a list in the dump. */
void
dump_definition_list_end()
{
	dump_html("</dl>\n","");
}

#ifdef DUMP_HTML_CSS_FILE
# ifdef DUMP_HTML_CSS_EMBEDDED
static
void
dump_html_css_file(const char *filename)
{
if (html_dump_fp) {
	FILE *css = fopen(filename, "r");
	if (!css) {
		pline("Can't open %s for input.", filename);
		pline("CSS file not included.");
	} else {
		int c=0;
		while ((c=fgetc(css))!=EOF) {
			fputc(c, html_dump_fp);
		}
		fclose(css);
	}
}
}
# endif
#endif


/** Dumps the HTML header. */
void
dump_header_html(title)
const char *title;
{
	dump_html("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n","");
	dump_html("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n", "");
	dump_html("<head>\n", "");
	dump_html("<title>NetHack " VERSION_STRING ": %s</title>\n", title);
	dump_html("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n", "");
	dump_html("<meta name=\"generator\" content=\"NetHack " VERSION_STRING "\" />\n", "");
#ifdef ANDROID
	dump_html("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n", "");
	dump_html("<meta name=\"format-detection\" content=\"telephone=no\">\n", "");
	dump_html("<link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/font-hack/2.020/css/hack-extended.min.css\"/>\n", "");
#endif
#ifdef DUMP_HTML_CSS_FILE
# ifndef DUMP_HTML_CSS_EMBEDDED
	dump_html("<link rel=\"stylesheet\" type=\"text/css\" href=\"" DUMP_HTML_CSS_FILE "\" />\n", "");
# else
	dump_html("<style type=\"text/css\">\n", "");
	dump_html_css_file(DUMP_HTML_CSS_FILE);
	dump_html("</style>\n", "");
# endif
#endif
	dump_html("</head>\n", "");
	dump_html("<body>\n", "");
}

static char html_escape_buf[BUFSZ];
/** Escape a single character for HTML. */
char* html_escape_character(const char c) {
	switch (c) {
		case '<':
			return "&lt;";
		case '>':
			return "&gt;";
		case '&':
			return "&amp;";
		case '\"':
			return "&quot;";
		case '\'':
			return "&#39;"; /* not &apos; */
		default:
			sprintf(html_escape_buf, "%c", c);
			return html_escape_buf;
	}
}

/*dump.c*/
