 
#ifndef WINANDROID_H_
#define WINANDROID_H_

void FDECL(and_init_nhwindows, (int *, char **));
void NDECL(and_player_selection);
void NDECL(and_askname);
void NDECL(and_get_nh_event) ;
void FDECL(and_exit_nhwindows, (const char *));
void FDECL(and_suspend_nhwindows, (const char *));
void NDECL(and_resume_nhwindows);
winid FDECL(and_create_nhwindow, (int));
void FDECL(and_clear_nhwindow, (winid));
void FDECL(and_display_nhwindow, (winid, BOOLEAN_P));
void FDECL(and_dismiss_nhwindow, (winid));
void FDECL(and_destroy_nhwindow, (winid));
void FDECL(and_curs, (winid,int,int));
void FDECL(and_putstr, (winid, int, const char *));
void FDECL(and_putmixed, (winid, int, const char *));
void FDECL(and_display_file, (const char *, BOOLEAN_P));
void FDECL(and_start_menu, (winid));
void FDECL(and_add_menu, (winid,int,const ANY_P *, CHAR_P,CHAR_P,int,const char *, BOOLEAN_P));
void FDECL(and_end_menu, (winid, const char *));
int FDECL(and_select_menu, (winid, int, MENU_ITEM_P **));
char FDECL(and_message_menu, (CHAR_P, int, const char *));
void NDECL(and_update_inventory);
void NDECL(and_mark_synch);
void NDECL(and_wait_synch);
#ifdef CLIPPING
void FDECL(and_cliparound, (int, int));
#endif
#ifdef POSITIONBAR
void FDECL(and_update_positionbar, (char *));
#endif
void FDECL(and_print_glyph, (winid,XCHAR_P,XCHAR_P,int,int));
void FDECL(and_raw_print, (const char *));
void FDECL(and_raw_print_bold, (const char *));
int NDECL(and_nhgetch);
int FDECL(and_nh_poskey, (int *, int *, int *));
void NDECL(and_nhbell);
int NDECL(and_doprev_message);
char FDECL(and_yn_function, (const char *, const char *, CHAR_P));
void FDECL(and_getlin, (const char *,char *));
void FDECL(and_getlin_log, (const char *,char *));
int NDECL(and_get_ext_cmd);
void FDECL(and_number_pad, (int));
void NDECL(and_delay_output);
#ifdef CHANGE_COLOR
void FDECL(and_change_color,(int color,long rgb,int reverse));
#ifdef MAC
void FDECL(and_change_background,(int white_or_black));
short FDECL(set_and_font_name, (winid, char *));
#endif
char * NDECL(and_get_color_string);
#endif
void NDECL(and_start_screen);
void NDECL(and_end_screen);
void FDECL(and_preference_update, (const char *));

int NetHackMain(int argc, char** argv);

#endif
