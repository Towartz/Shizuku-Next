#include <sys/types.h>
#include <sys/sendfile.h>
#include <sys/stat.h>
#include <zconf.h>
#include <dirent.h>
#include <fcntl.h>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <sched.h>
#include <cctype>
#include <cerrno>
#include "misc.h"

ssize_t fdgets(char *buf, const size_t size, int fd) {
    buf[0] = '\0';
    ssize_t ret;
    do {
        ret = read(fd, buf, size - 1);
    } while (ret < 0 && errno == EINTR);
    if (ret < 0)
        return -1;
    buf[ret] = '\0';
    return ret;
}

int get_proc_name(int pid, char *name, size_t size) {
    int fd;
    char buf[PATH_MAX];
    snprintf(buf, sizeof(buf), "/proc/%d/cmdline", pid);
    if ((fd = open(buf, O_RDONLY)) == -1)
        return 1;
    fdgets(name, size, fd);
    close(fd);
    return 0;
}

int is_shizuku_server(int pid, const char *target_name) {
    if (pid <= 1 || pid == getpid()) return 0;

    char buf[4096];

    // 1. Check /proc/<pid>/comm (process thread name, max 15 chars in Linux kernel)
    snprintf(buf, sizeof(buf), "/proc/%d/comm", pid);
    int fd = open(buf, O_RDONLY);
    if (fd != -1) {
        ssize_t n = fdgets(buf, sizeof(buf), fd);
        close(fd);
        if (n > 0) {
            trim(buf);
            if (strcmp(buf, "shizuku_server") == 0 ||
                strcmp(buf, "shizuku_serve") == 0 ||
                (target_name && strcmp(buf, target_name) == 0)) {
                return 1;
            }
        }
    }

    // 2. Check full /proc/<pid>/cmdline across all null-separated arguments
    snprintf(buf, sizeof(buf), "/proc/%d/cmdline", pid);
    fd = open(buf, O_RDONLY);
    if (fd != -1) {
        ssize_t n = read(fd, buf, sizeof(buf) - 1);
        close(fd);
        if (n > 0) {
            buf[n] = '\0';
            for (ssize_t i = 0; i < n; ++i) {
                if (buf[i] == '\0') buf[i] = ' ';
            }
            if (strstr(buf, "rikka.shizuku.server.ShizukuService") != nullptr ||
                strstr(buf, "--nice-name=shizuku_server") != nullptr ||
                strstr(buf, "shizuku_server") != nullptr ||
                (target_name && strstr(buf, target_name) != nullptr)) {
                return 1;
            }
        }
    }

    // 3. Check /proc/<pid>/stat (extract process comm within parentheses)
    snprintf(buf, sizeof(buf), "/proc/%d/stat", pid);
    fd = open(buf, O_RDONLY);
    if (fd != -1) {
        ssize_t n = fdgets(buf, sizeof(buf), fd);
        close(fd);
        if (n > 0) {
            char *open_p = strchr(buf, '(');
            char *close_p = strrchr(buf, ')');
            if (open_p && close_p && close_p > open_p) {
                *close_p = '\0';
                char *comm = open_p + 1;
                if (strcmp(comm, "shizuku_server") == 0 ||
                    strcmp(comm, "shizuku_serve") == 0 ||
                    (target_name && strcmp(comm, target_name) == 0)) {
                    return 1;
                }
            }
        }
    }

    return 0;
}

int is_num(const char *s) {
    if (!s || !*s) return 0;
    while (*s) {
        if (*s < '0' || *s > '9')
            return 0;
        s++;
    }
    return 1;
}

int copyfileat(int src_path_fd, const char *src_path, int dst_path_fd, const char *dst_path) {
    int src_fd;
    int dst_fd;
    struct stat stat_buf{};
    int64_t size_remaining;
    size_t count;
    ssize_t result;

    if ((src_fd = openat(src_path_fd, src_path, O_RDONLY)) == -1)
        return -1;

    if (fstat(src_fd, &stat_buf) == -1)
        return -1;

    dst_fd = openat(dst_path_fd, dst_path, O_WRONLY | O_CREAT | O_TRUNC, stat_buf.st_mode);
    if (dst_fd == -1) {
        close(src_fd);
        return -1;
    }

    size_remaining = stat_buf.st_size;
    for (;;) {
        if (size_remaining > 0x7ffff000)
            count = 0x7ffff000;
        else
            count = static_cast<size_t>(size_remaining);

        result = sendfile(dst_fd, src_fd, nullptr, count);
        if (result == -1) {
            close(src_fd);
            close(dst_fd);
            unlink(dst_path);
            return -1;
        }

        size_remaining -= result;
        if (size_remaining == 0) {
            close(src_fd);
            close(dst_fd);
            return 0;
        }
    }
}

int copyfile(const char *src_path, const char *dst_path) {
    return copyfileat(0, src_path, 0, dst_path);
}

uintptr_t memsearch(const uintptr_t start, const uintptr_t end, const void *value, size_t size) {
    if (start >= end || size == 0 || (end - start) < size)
        return 0;

    const void *found = memmem((const void *) start, end - start, value, size);
    return found ? (uintptr_t) found : 0;
}

int switch_mnt_ns(int pid) {
    char mnt[32];
    snprintf(mnt, sizeof(mnt), "/proc/%d/ns/mnt", pid);
    if (access(mnt, R_OK) == -1) return -1;

    int fd = open(mnt, O_RDONLY);
    if (fd < 0) return -1;

    int res = setns(fd, 0);
    close(fd);
    return res;
}

void foreach_proc(foreach_proc_function *func) {
    DIR *dir;
    struct dirent *entry;

    if (!(dir = opendir("/proc")))
        return;

    while ((entry = readdir(dir))) {
        if (entry->d_type != DT_DIR) continue;
        if (!is_num(entry->d_name)) continue;
        pid_t pid = atoi(entry->d_name);
        func(pid);
    }

    closedir(dir);
}

char *trim(char *str) {
    if (str == nullptr || *str == '\0') { return str; }

    char *frontp = str;
    while (isspace((unsigned char) *frontp)) { ++frontp; }

    char *endp = str + strlen(str) - 1;
    while (endp >= frontp && isspace((unsigned char) *endp)) { --endp; }

    size_t new_len = endp >= frontp ? (size_t)(endp - frontp + 1) : 0;
    if (frontp != str && new_len > 0) {
        memmove(str, frontp, new_len);
    }
    str[new_len] = '\0';

    return str;
}
