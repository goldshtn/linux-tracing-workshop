#include <jvmti.h>
#include <iostream>
#include <string>

class monitor_tracker {
public:
    static monitor_tracker& instance() {
        static monitor_tracker s_instance;
        return s_instance;
    }
    void attach(JavaVM *vm);
    void enter_start(jthread thread, jobject object);
    void enter_end(jthread thread, jobject object);
    void dump();
private:
    bool attached_ = false;
};

void JNICALL MonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
                                   jthread thread, jobject object) {
    monitor_tracker::instance().enter_start(thread, object);
}

void JNICALL MonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
                                     jthread thread, jobject object) {
    monitor_tracker::instance().enter_end(thread, object);
}

void monitor_tracker::enter_start(jthread thread, jobject object) {
    // TODO
}

void monitor_tracker::enter_end(jthread thread, jobject object) {
    // TODO
}

void monitor_tracker::dump() {
    // TODO
    std::cout << "[AGENT] dumping monitor contention stats\n";
}

void monitor_tracker::attach(JavaVM *vm) {
    if (attached_)
        return;

    attached_ = true;
    std::cout << "[AGENT] attaching to monitor events\n";

    jvmtiEnv *jvmti;
    vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);

    jvmtiCapabilities capabilities = {0};
    capabilities.can_generate_monitor_events = 1;
    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks = {0};
    callbacks.MonitorContendedEnter = MonitorContendedEnter;
    callbacks.MonitorContendedEntered = MonitorContendedEntered;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));

    jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                    JVMTI_EVENT_MONITOR_CONTENDED_ENTER,
                                    NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                    JVMTI_EVENT_MONITOR_CONTENDED_ENTERED,
                                    NULL);
}

void do_options(JavaVM *vm, char *options) {
    if (std::string(options) == "dump") {
        monitor_tracker::instance().dump();
    } else {
        monitor_tracker::instance().attach(vm);
    }
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    do_options(vm, options);
    return JNI_OK;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
}

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    do_options(vm, options);
    return JNI_OK;
}

