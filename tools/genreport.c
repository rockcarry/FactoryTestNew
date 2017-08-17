#include <windows.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <conio.h>

static char* find_section(char *buf, char *name)
{
    char *section = strstr(buf, name);
    if (section) {
        return (section + strlen(name));
    } else {
        return NULL;
    }
}

static void read_data(char *section, char *key, char *data)
{
    char *str = NULL;
    int   i;
    if (!section) return;
    str = strstr(section, key);
    if (!str) return;
    else str += strlen(key);
    for (; *str && *str != '\r' && *str != '\n'; str++) {
        if (*str == ':') break;
    }
    if (*str != ':') return;
    for (str++; *str == ' '; str++);
    for (; *str && *str != '\r' && *str != '\n'; ) {
        *data++ = *str++;
    }
    *data = '\0';
}

static int get_test_result(char *file,
                char *gps_cn, char *gps_res,
                char *wifi_mac, char *wifi_signal, char *wifi_name, char *wifi_res,
                char *bt_mac, char *bt_signal, char *bt_name, char *bt_res,
                char *extsd_res, char *uhost_res, char *hpdet_res,
                char *gsensor_data, char *gsensor_res,
                char *bat_status, char *bat_res,
                char *ddr_size, char *ddr_res, char *flash_size, char *flash_res,
                char *home_res, char *power_res, char *volinc_res, char *voldec_res,
                char *spk_res, char *ear_res, char *bkl_res, char *led_res)
{
    char *buf      = NULL;
    int   len      = 0;
    FILE *fp       = NULL;
    char *section  = NULL;
    char  str0[256]= "";
    char  str1[64] = "";
    char  str2[64] = "";
    char  str3[64] = "";
    int   ret      = -1;

    // set default value
    strcpy(gps_cn      , "unknown");
    strcpy(gps_res     , "unknown");
    strcpy(wifi_mac    , "unknown");
    strcpy(wifi_signal , "unknown");
    strcpy(wifi_name   , "unknown");
    strcpy(wifi_res    , "unknown");
    strcpy(bt_mac      , "unknown");
    strcpy(bt_signal   , "unknown");
    strcpy(bt_name     , "unknown");
    strcpy(bt_res      , "unknown");
    strcpy(extsd_res   , "unknown");
    strcpy(uhost_res   , "unknown");
    strcpy(hpdet_res   , "unknown");
    strcpy(gsensor_data, "unknown");
    strcpy(gsensor_res , "unknown");
    strcpy(bat_status  , "unknown");
    strcpy(bat_res     , "unknown");
    strcpy(ddr_size    , "unknown");
    strcpy(ddr_res     , "unknown");
    strcpy(flash_size  , "unknown");
    strcpy(flash_res   , "unknown");
    strcpy(home_res    , "unknown");
    strcpy(power_res   , "unknown");
    strcpy(volinc_res  , "unknown");
    strcpy(voldec_res  , "unknown");
    strcpy(spk_res     , "unknown");
    strcpy(ear_res     , "unknown");
    strcpy(bkl_res     , "unknown");
    strcpy(led_res     , "unknown");

    // open file
    fp = fopen(file, "rb");
    if (!fp) goto done;
    

    //++ read file
    fseek(fp, 0, SEEK_END);
    len = ftell(fp);
    buf = malloc(len);
    if (!len || !buf) goto done;

    fseek(fp, 0, SEEK_SET);
    fread(buf, len, 1, fp);
    //-- read file

    //++ read data by section and key
    // gps
    section = find_section(buf, "GPS test");
    read_data(section, "satellite snr", gps_cn);
    read_data(section, "test result", gps_res);

    // wifi
    section = find_section(buf, "WiFi test");
    read_data(section, "mac", wifi_mac);
    read_data(section, "highest signal level", str0);
    sscanf(str0, "%s %s", wifi_signal, wifi_name);
    read_data(section, "test result", wifi_res);

    // bt
    section = find_section(buf, "Bluetooth test");
    read_data(section, "mac", bt_mac);
    read_data(section, "highest signal level", str0);
    sscanf(str0, "%s %s", bt_signal, bt_name);
    read_data(section, "test result", bt_res);
    //-- read data by section and key

    // device test
    section = find_section(buf, "Device test");
    read_data(section, "extsd", str0); sscanf(str0, "%s %s", extsd_res, str1);
    read_data(section, "uhost", str0); sscanf(str0, "%s %s", uhost_res, str1);
    read_data(section, "hpdet", str0); sscanf(str0, "%s %s", hpdet_res, str1);

    read_data(section, "gsensor", str0);
    sscanf(str0, "%s %s %s %s", gsensor_res, gsensor_data, str1, str2);
    strcat(gsensor_data, str1);
    strcat(gsensor_data, str2);

    read_data(section, "battery", str0);
    sscanf(str0, "%s %s %s %s", bat_res, str1, str2, str3);
    sprintf(bat_status, "%s %s %s", str1, str2, str3);

    read_data(section, "ddr", str0);
    sscanf(str0, "%s %s %s", ddr_res, str1, str2);
    sprintf(ddr_size, "%s %s", str1, str2);

    read_data(section, "flash", str0);
    sscanf(str0, "%s %s %s", flash_res, str1, str2);
    sprintf(flash_size, "%s %s", str1, str2);

    section = find_section(buf, "Button test");
    read_data(section, "home" , str0); sscanf(str0, "%s %s", home_res, str1);
    read_data(section, "power", str0); sscanf(str0, "%s %s", power_res, str1);
    read_data(section, "vol-" , str0); sscanf(str0, "%s %s", volinc_res, str1);
    read_data(section, "vol+" , str0); sscanf(str0, "%s %s", voldec_res, str1);

    section = find_section(buf, "Other test");
    read_data(section, "speaker test" , spk_res);
    read_data(section, "earphone test", ear_res);
    read_data(section, "backlight test" , bkl_res);
    read_data(section, "chargeled test" , led_res);

    // ok
    ret = 0;

done:    
    if (fp ) fclose(fp);
    if (buf) free(buf);
    return ret;
}

static void traverse_dir(char *path)
{
    WIN32_FIND_DATA wfd = {0};
    HANDLE        hfind = NULL;
    FILE            *fp = NULL;

    char  file[MAX_PATH]  = "";
    char  sn[16]          = "";
    char  gps_cn[50]      = "";
    char  gps_res[10]     = "";
    char  wifi_mac[18]    = "";
    char  wifi_signal[10] = "";
    char  wifi_name[20]   = "";
    char  wifi_res[10]    = "";
    char  bt_mac[18]      = "";
    char  bt_signal[10]   = "";
    char  bt_name[20]     = "";
    char  bt_res[10]      = "";
    char  extsd_res[10]   = "";
    char  uhost_res[10]   = "";
    char  hpdet_res[10]   = "";
    char  gsensor_data[20]= "";
    char  gsensor_res[10] = "";
    char  bat_status[30]  = "";
    char  bat_res[10]     = "";
    char  ddr_size[16]    = "";
    char  ddr_res[10]     = "";
    char  flash_size[16]  = "";
    char  flash_res[10]   = "";
    char  home_res[10]    = "";
    char  power_res[10]   = "";
    char  volinc_res[10]  = "";
    char  voldec_res[10]  = "";
    char  spk_res[10]     = "";
    char  ear_res[10]     = "";
    char  bkl_res[10]     = "";
    char  led_res[10]     = "";

    fp = fopen("testreport.csv", "wb");
    if (!fp) goto done;
    fprintf(fp, "sn,gps_cn,gps_result,wifi_mac,wifi_highest_signal,wifi_ap,wifi_result,bt_mac,bt_highest_signal,bt_dev,bt_result,extsd,uhost,hpdet,gsensor_value,gsensor_result,bat_status,bat_result,ddr_size,ddr_result,flash_size,flash_result,home,power,vol+,vol-,speaker,earphone,backlight,charge_led\r\n");

    strcpy(file, path);
    strcat(file, "\\*");

    hfind = FindFirstFile(file, &wfd);
    if (hfind == INVALID_HANDLE_VALUE) goto done;

    while (1) {
        if (!(wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
            int   len, ret;
            char *str;
            strcpy(file, path);
            strcat(file, "\\");
            strcat(file, wfd.cFileName);

            strcpy(sn, wfd.cFileName);
            len = strlen(sn);
            if (len < 5 || stricmp(sn + len - 4, ".txt") != 0) {
                goto next;
            } else {
                sn[len - 4] = '\0';
                str = strstr(sn, "-");
                if (str) strcpy(sn, str+1);
            }

            ret = get_test_result(file,
                    gps_cn, gps_res,
                    wifi_mac, wifi_signal, wifi_name, wifi_res,
                    bt_mac, bt_signal, bt_name, bt_res,
                    extsd_res, uhost_res, hpdet_res,
                    gsensor_data, gsensor_res,
                    bat_status, bat_res,
                    ddr_size, ddr_res, flash_size, flash_res,
                    home_res, power_res, volinc_res, voldec_res,
                    spk_res, ear_res, bkl_res, led_res);
            fprintf(fp, "T54-%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\r\n", sn,
                    gps_cn, gps_res,
                    wifi_mac, wifi_signal, wifi_name, wifi_res,
                    bt_mac, bt_signal, bt_name, bt_res,
                    extsd_res, uhost_res, hpdet_res,
                    gsensor_data, gsensor_res,
                    bat_status, bat_res,
                    ddr_size, ddr_res, flash_size, flash_res,
                    home_res, power_res, volinc_res, voldec_res,
                    spk_res, ear_res, bkl_res, led_res);
            printf("%s %s\r\n", sn, ret == 0 ? "ok" : "ng xxxxx");
        }
next:
        if (!FindNextFile(hfind, &wfd)) break;
    }

done:
    if (fp   ) fclose(fp);
    if (hfind) FindClose(hfind);
}

int main(int argc, char *argv[])
{
    FILE *fp = NULL;
    char  path[MAX_PATH];

    if (argc < 2) {
        strcpy(path, ".");
    } else {
        strcpy(path, argv[1]);
    }

    traverse_dir(path);
    getch();
    return 0;
}
