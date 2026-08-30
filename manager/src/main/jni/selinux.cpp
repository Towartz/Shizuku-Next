#include <fcntl.h>
#include <cstring>
#include <unistd.h>
#include <dlfcn.h>
#include <cerrno>
#include <syscall.h>
#include <cstdlib>
#include "selinux.h"

namespace se {

    static int __getcon(char **context) {
        int fd = open("/proc/self/attr/current", O_RDONLY | O_CLOEXEC);
        if (fd < 0)
            return fd;

        char *buf;
        size_t size;
        int errno_hold;
        ssize_t ret;

        size = sysconf(_SC_PAGE_SIZE);
        buf = (char *) malloc(size);
        if (!buf) {
            ret = -1;
            goto out;
        }
        memset(buf, 0, size);

        do {
            ret = read(fd, buf, size - 1);
        } while (ret < 0 && errno == EINTR);
        if (ret < 0)
            goto out2;

        if (ret == 0) {
            *context = nullptr;
            goto out2;
        }

        *context = strdup(buf);
        if (!(*context)) {
            ret = -1;
            goto out2;
        }
        ret = 0;
        out2:
        free(buf);
        out:
        errno_hold = errno;
        close(fd);
        errno = errno_hold;
        return ret;
    }

    static int __setcon(const char *ctx) {
        int fd = open("/proc/self/attr/current", O_WRONLY | O_CLOEXEC);
        if (fd < 0)
            return fd;
        size_t len = strlen(ctx) + 1;
        ssize_t rc = write(fd, ctx, len);
        close(fd);
        return rc != (ssize_t) len;
    }

    static int __setfilecon(const char *path, const char *ctx) {
        int rc = syscall(__NR_setxattr, path, "security.selinux"/*XATTR_NAME_SELINUX*/, ctx,
                         strlen(ctx) + 1, 0);
        if (rc) {
            errno = -rc;
            return -1;
        }
        return 0;
    }

    static int __selinux_check_access(const char *scon, const char *tcon,
                                      const char *tclass, const char *perm, void *auditdata) {
        return 0;
    }

    static void __freecon(char *con) {
        free(con);
    }

    getcon_t *getcon = __getcon;
    setcon_t *setcon = __setcon;
    setfilecon_t *setfilecon = __setfilecon;
    selinux_check_access_t *selinux_check_access = __selinux_check_access;
    freecon_t *freecon = __freecon;

    void init() {
        if (access("/system/lib/libselinux.so", F_OK) != 0 && access("/system/lib64/libselinux.so", F_OK) != 0)
            return;

        void *handle = dlopen("libselinux.so", RTLD_LAZY | RTLD_LOCAL);
        if (handle == nullptr)
            return;

        auto gc = (getcon_t *) dlsym(handle, "getcon");
        if (gc) getcon = gc;

        auto sc = (setcon_t *) dlsym(handle, "setcon");
        if (sc) setcon = sc;

        auto sfc = (setfilecon_t *) dlsym(handle, "setfilecon");
        if (sfc) setfilecon = sfc;

        auto sca = (selinux_check_access_t *) dlsym(handle, "selinux_check_access");
        if (sca) selinux_check_access = sca;

        auto fc = (freecon_t *) dlsym(handle, "freecon");
        if (fc) freecon = fc;
    }
}
