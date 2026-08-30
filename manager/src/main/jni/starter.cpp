#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <ctime>
#include <cstring>
#include <libgen.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <cerrno>
#include <string>
#include <termios.h>
#include <vector>
#include <string>
#include <regex>
#include <random>
#include "android.h"
#include "misc.h"
#include "selinux.h"
#include "cgroup.h"
#include "logging.h"

#ifdef DEBUG
#define JAVA_DEBUGGABLE
#endif

#define perrorf(...) fprintf(stderr, __VA_ARGS__)

#define EXIT_FATAL_SET_CLASSPATH 3
#define EXIT_FATAL_FORK 4
#define EXIT_FATAL_APP_PROCESS 5
#define EXIT_FATAL_UID 6
#define EXIT_FATAL_PM_PATH 7
#define EXIT_FATAL_KILL 9
#define EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX 10

#define PACKAGE_NAME "moe.shizuku.privileged.api"
#define SERVER_NAME "shizuku_server"
#define SERVER_CLASS_PATH "rikka.shizuku.server.ShizukuService"

#if defined(__arm__)
#define ABI "arm"
#elif defined(__i386__)
#define ABI "x86"
#elif defined(__x86_64__)
#define ABI "x86_64"
#elif defined(__aarch64__)
#define ABI "arm64"
#endif

static char s_target_process_name[1024] = SERVER_NAME;

static void run_server(const char *dex_path, const char *main_class, const char *process_name, const char *manager_package) {
    if (setenv("CLASSPATH", dex_path, true)) {
        LOGE("can't set CLASSPATH\n");
        exit(EXIT_FATAL_SET_CLASSPATH);
    }

    std::string dex_str(dex_path);
    std::string dir_str = ".";
    auto last_slash = dex_str.find_last_of('/');
    if (last_slash != std::string::npos) {
        dir_str = dex_str.substr(0, last_slash);
    }
    std::string lib_path = dir_str + "/lib/" + ABI;

    std::vector<std::string> args;
    args.reserve(16);

    args.emplace_back("/system/bin/app_process");
    args.emplace_back(std::string("-Djava.class.path=") + dex_path);
    args.emplace_back(std::string("-Dshizuku.library.path=") + lib_path);
    args.emplace_back(std::string("-Dshizuku.manager.package=") + manager_package);

#ifdef JAVA_DEBUGGABLE
    if (android_get_device_api_level() >= 30) {
        args.emplace_back("-Xcompiler-option");
        args.emplace_back("--debuggable");
        args.emplace_back("-XjdwpProvider:adbconnection");
        args.emplace_back("-XjdwpOptions:suspend=n,server=y");
    } else if (android_get_device_api_level() >= 28) {
        args.emplace_back("-Xcompiler-option");
        args.emplace_back("--debuggable");
        args.emplace_back("-XjdwpProvider:internal");
        args.emplace_back("-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y");
    } else {
        args.emplace_back("-Xcompiler-option");
        args.emplace_back("--debuggable");
        args.emplace_back("-agentlib:jdwp=transport=dt_android_adb,suspend=n,server=y");
    }
#endif

    args.emplace_back("/system/bin");
    args.emplace_back(std::string("--nice-name=") + process_name);
    args.emplace_back(main_class);

#ifdef JAVA_DEBUGGABLE
    args.emplace_back("--debug");
#endif

    std::vector<char *> argv;
    argv.reserve(args.size() + 1);
    for (auto &arg : args) {
        argv.push_back(const_cast<char *>(arg.c_str()));
    }
    argv.push_back(nullptr);

    LOGD("exec app_process");

    if (execvp(argv[0], argv.data())) {
        exit(EXIT_FATAL_APP_PROCESS);
    }
}

static void start_server(const char *path, const char *main_class, const char *process_name, const char *manager_package) {
    pid_t pid = fork();
    switch (pid) {
        case -1: {
            perrorf("fatal: can't fork\n");
            exit(EXIT_FATAL_FORK);
        }
        case 0: {
            LOGD("child");
            setsid();
            chdir("/");
            int fd = open("/dev/null", O_RDWR);
            if (fd != -1) {
                dup2(fd, STDIN_FILENO);
                dup2(fd, STDOUT_FILENO);
                dup2(fd, STDERR_FILENO);
                if (fd > 2) close(fd);
            }
            run_server(path, main_class, process_name, manager_package);
        }
        default: {
            printf("info: %s pid is %d\n", process_name, pid);
            printf("info: shizuku_starter exit with 0\n");
            exit(EXIT_SUCCESS);
        }
    }
}

static int check_selinux(const char *s, const char *t, const char *c, const char *p) {
    int res = se::selinux_check_access(s, t, c, p, nullptr);
#ifndef DEBUG
    if (res != 0) {
#endif
    printf("info: selinux_check_access %s %s %s %s: %d\n", s, t, c, p, res);
    fflush(stdout);
#ifndef DEBUG
    }
#endif
    return res;
}

static int switch_cgroup() {
    int pid = getpid();
    if (cgroup::switch_cgroup("/acct", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /acct\n");
        return 0;
    }
    if (cgroup::switch_cgroup("/dev/cg2_bpf", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /dev/cg2_bpf\n");
        return 0;
    }
    if (cgroup::switch_cgroup("/sys/fs/cgroup", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /sys/fs/cgroup\n");
        return 0;
    }
    if (cgroup::switch_cgroup("/dev/cpuset", pid)) {
        printf("info: switch cgroup succeeded, cgroup in /dev/cpuset\n");
        return 0;
    }
    char buf[PROP_VALUE_MAX + 1];
    if (__system_property_get("ro.config.per_app_memcg", buf) > 0 &&
        strncmp(buf, "false", 5) != 0) {
        if (cgroup::switch_cgroup("/dev/memcg/apps", pid)) {
            printf("info: switch cgroup succeeded, cgroup in /dev/memcg/apps\n");
            return 0;
        }
    }
    printf("warn: can't switch cgroup\n");
    fflush(stdout);
    return -1;
}

int main(int argc, char *argv[]) {
    std::string apk_path;
    std::string process_name = SERVER_NAME;
    std::string manager_package = PACKAGE_NAME;
    for (int i = 0; i < argc; ++i) {
        if (strncmp(argv[i], "--apk=", 6) == 0) {
            apk_path = argv[i] + 6;
        } else if (strncmp(argv[i], "--process-name=", 15) == 0) {
            process_name = argv[i] + 15;
        } else if (strncmp(argv[i], "--manager-package=", 18) == 0) {
            manager_package = argv[i] + 18;
        }
    }

    snprintf(s_target_process_name, sizeof(s_target_process_name), "%s", process_name.c_str());

    uid_t uid = getuid();
    if (uid != 0 && uid != 2000) {
        perrorf("fatal: run Shizuku from non root nor adb user (uid=%d).\n", uid);
        exit(EXIT_FATAL_UID);
    }

    se::init();

    char *context = nullptr;
    if (se::getcon && se::getcon(&context) == 0) {
        printf("info: starter from %s\n", context);
        if (se::freecon) se::freecon(context);
    }

    if (uid == 0) {
        switch_cgroup();
    }

    if (check_selinux("u:r:shell:s0", "u:r:shell:s0", "binder", "call") != 0) {
        exit(EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX);
    }

    if (check_selinux("u:r:shell:s0", "u:r:shell:s0", "binder", "transfer") != 0) {
        exit(EXIT_FATAL_BINDER_BLOCKED_BY_SELINUX);
    }

    check_selinux("u:r:untrusted_app:s0", "u:r:shell:s0", "binder", "call");
    check_selinux("u:r:untrusted_app:s0", "u:r:shell:s0", "binder", "transfer");
    check_selinux("u:r:untrusted_app_25:s0", "u:r:shell:s0", "binder", "call");
    check_selinux("u:r:untrusted_app_25:s0", "u:r:shell:s0", "binder", "transfer");
    check_selinux("u:r:untrusted_app_27:s0", "u:r:shell:s0", "binder", "call");
    check_selinux("u:r:untrusted_app_27:s0", "u:r:shell:s0", "binder", "transfer");
    check_selinux("u:r:untrusted_app_29:s0", "u:r:shell:s0", "binder", "call");
    check_selinux("u:r:untrusted_app_29:s0", "u:r:shell:s0", "binder", "transfer");

    printf("info: starter begin\n");
    fflush(stdout);

    // kill old server
    printf("info: killing old process...\n");
    fflush(stdout);

    int killed_count = 0;
    foreach_proc([&killed_count](pid_t pid) {
        if (pid == getpid()) return;

        if (!is_shizuku_server(pid, s_target_process_name))
            return;

        if (kill(pid, SIGKILL) == 0) {
            printf("info: killed %d (%s)\n", pid, s_target_process_name);
            killed_count++;
        } else if (errno == EPERM) {
            perrorf("fatal: can't kill %d, please try to stop existing Shizuku from app first.\n", pid);
            exit(EXIT_FATAL_KILL);
        } else {
            printf("warn: failed to kill %d\n", pid);
        }
    });

    if (killed_count == 0) {
        printf("info: no lingering processes found (clean state)\n");
    } else {
        printf("info: cleanly terminated %d lingering process(es)\n", killed_count);
    }
    fflush(stdout);

    // Brief yield to allow OS to reclaim process table and binder descriptors
    usleep(50000);

    if (access(apk_path.c_str(), R_OK) == 0) {
        printf("info: use apk path from argv\n");
        fflush(stdout);
    }

    if (apk_path.empty()) {
        std::string cmd = "pm path " + manager_package;
        auto f = popen(cmd.c_str(), "r");
        if (f) {
            char line[PATH_MAX]{0};
            fgets(line, PATH_MAX, f);
            trim(line);
            if (strstr(line, "package:") == line) {
                apk_path = line + strlen("package:");
            }
            pclose(f);
        }
    }

    if (apk_path.empty()) {
        perrorf("fatal: can't get path of manager\n");
        exit(EXIT_FATAL_PM_PATH);
    }

    printf("info: apk path is %s\n", apk_path.c_str());
    if (access(apk_path.c_str(), R_OK) != 0) {
        perrorf("fatal: can't access manager %s\n", apk_path.c_str());
        exit(EXIT_FATAL_PM_PATH);
    }

    printf("info: starting server...\n");
    fflush(stdout);

    start_server(apk_path.c_str(), SERVER_CLASS_PATH, process_name.c_str(), manager_package.c_str());

    return 0;
}
