-------------------------------------
Translated Report (Full Report Below)
-------------------------------------
Process:             java [37880]
Path:                /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/bin/java
Identifier:          java
Version:             21.0.4 (7)
Code Type:           ARM-64 (Native)
Role:                Foreground
Parent Process:      java [37371]
Coalition:           com.google.antigravity [1338]
Responsible Process: Electron [3219]
User ID:             501

Date/Time:           2025-12-30 15:49:09.0339 +0100
Launch Time:         2025-12-30 15:48:55.4988 +0100
Hardware Model:      MacBookPro18,3
OS Version:          macOS 26.1 (25B78)
Release Type:        User

Crash Reporter Key:  328A6717-B4DA-BF04-BD7F-76EAA080F4D0
Incident Identifier: 17EF0077-0278-4F40-B761-D18414904758

Sleep/Wake UUID:       85693A8E-4302-4AA6-9A51-C19FB9429057

Time Awake Since Boot: 13000 seconds
Time Since Wake:       1074 seconds

System Integrity Protection: enabled

Triggered by Thread: 29  Java: AWT-EventQueue-0

Exception Type:    EXC_BREAKPOINT (SIGTRAP)
Exception Codes:   0x0000000000000001, 0x00000001abbc140c

Termination Reason:  Namespace SIGNAL, Code 5, Trace/BPT trap: 5
Terminating Process: exc handler [37880]


Thread 0::  Dispatch queue: com.apple.main-thread
0   libsystem_kernel.dylib        	       0x182262c34 mach_msg2_trap + 8
1   libsystem_kernel.dylib        	       0x182275028 mach_msg2_internal + 76
2   libsystem_kernel.dylib        	       0x18226b98c mach_msg_overwrite + 484
3   libsystem_kernel.dylib        	       0x182262fb4 mach_msg + 24
4   CoreFoundation                	       0x182344b90 __CFRunLoopServiceMachPort + 160
5   CoreFoundation                	       0x1823434e8 __CFRunLoopRun + 1188
6   CoreFoundation                	       0x1823fd35c _CFRunLoopRunSpecificWithOptions + 532
7   HIToolbox                     	       0x18ee00768 RunCurrentEventLoopInMode + 316
8   HIToolbox                     	       0x18ee03a90 ReceiveNextEventCommon + 488
9   HIToolbox                     	       0x18ef8d308 _BlockUntilNextEventMatchingListInMode + 48
10  AppKit                        	       0x186c543c0 _DPSBlockUntilNextEventMatchingListInMode + 236
11  AppKit                        	       0x18674de34 _DPSNextEvent + 588
12  AppKit                        	       0x18721bf44 -[NSApplication(NSEventRouting) _nextEventMatchingEventMask:untilDate:inMode:dequeue:] + 688
13  AppKit                        	       0x18721bc50 -[NSApplication(NSEventRouting) nextEventMatchingMask:untilDate:inMode:dequeue:] + 72
14  libosxapp.dylib               	       0x106163748 -[NSApplicationAWT nextEventMatchingMask:untilDate:inMode:dequeue:] + 136
15  AppKit                        	       0x186746780 -[NSApplication run] + 368
16  libosxapp.dylib               	       0x106163568 +[NSApplicationAWT runAWTLoopWithApp:] + 132
17  libawt_lwawt.dylib            	       0x124d2f42c +[AWTStarter starter:headless:] + 376
18  libosxapp.dylib               	       0x106164d88 +[ThreadUtilities invokeBlockCopy:] + 28
19  Foundation                    	       0x183b67114 __NSThreadPerformPerform + 264
20  CoreFoundation                	       0x1823449e8 __CFRUNLOOP_IS_CALLING_OUT_TO_A_SOURCE0_PERFORM_FUNCTION__ + 28
21  CoreFoundation                	       0x18234497c __CFRunLoopDoSource0 + 172
22  CoreFoundation                	       0x1823446e8 __CFRunLoopDoSources0 + 232
23  CoreFoundation                	       0x182343378 __CFRunLoopRun + 820
24  CoreFoundation                	       0x1823fd35c _CFRunLoopRunSpecificWithOptions + 532
25  libjli.dylib                  	       0x100450d88 CreateExecutionEnvironment + 404
26  libjli.dylib                  	       0x10044c3f8 JLI_Launch + 1152
27  java                          	       0x100407bb8 main + 404
28  dyld                          	       0x181eddd54 start + 7184

Thread 1:
0   libsystem_kernel.dylib        	       0x182264ae4 __ulock_wait + 8
1   libsystem_pthread.dylib       	       0x1822a80ac _pthread_join + 608
2   libjli.dylib                  	       0x10045146c CallJavaMainInNewThread + 184
3   libjli.dylib                  	       0x10044fed0 ContinueInNewThread + 148
4   libjli.dylib                  	       0x10044d8f0 JLI_Launch + 6520
5   java                          	       0x100407bb8 main + 404
6   libjli.dylib                  	       0x100451d48 apple_main + 88
7   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
8   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 2:
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067f15d8 jni_invoke_static(JNIEnv_*, JavaValue*, _jobject*, JNICallType, _jmethodID*, JNI_ArgumentPusher*, JavaThread*) + 380
18  libjvm.dylib                  	       0x1067f4fd4 jni_CallStaticVoidMethod + 276
19  libjli.dylib                  	       0x10044e6e4 JavaMain + 2320
20  libjli.dylib                  	       0x1004514b4 ThreadJavaMain + 12
21  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
22  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 3:: Java: GC Thread#0
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 4:: Java: G1 Main Marker
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af93dc PlatformMonitor::wait(unsigned long long) + 144
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106686124 G1ConcurrentMarkThread::run_service() + 156
5   libjvm.dylib                  	       0x10657e2b8 ConcurrentGCThread::run() + 36
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 5:: Java: G1 Conc#0
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 6:: Java: G1 Refine#0
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x10668effc G1PrimaryConcurrentRefineThread::wait_for_completed_buffers() + 72
5   libjvm.dylib                  	       0x10668eb04 G1ConcurrentRefineThread::run_service() + 136
6   libjvm.dylib                  	       0x10657e2b8 ConcurrentGCThread::run() + 36
7   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
8   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
9   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
10  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 7:: Java: G1 Service
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x1066d430c G1ServiceThread::wait_for_task() + 172
5   libjvm.dylib                  	       0x1066d4520 G1ServiceThread::run_service() + 40
6   libjvm.dylib                  	       0x10657e2b8 ConcurrentGCThread::run() + 36
7   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
8   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
9   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
10  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 8:: Java: VM Periodic Task Thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106accf38 WatcherThread::sleep() const + 152
5   libjvm.dylib                  	       0x106acd014 WatcherThread::run() + 60
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 9:: Java: VM Thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106d4df10 VMThread::wait_for_operation() + 444
5   libjvm.dylib                  	       0x106d4d0fc VMThread::run() + 188
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 10:

Thread 11:: Java: Reference Handler
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af93dc PlatformMonitor::wait(unsigned long long) + 144
3   libjvm.dylib                  	       0x106ab6ed0 Monitor::wait(unsigned long long) + 124
4   libjvm.dylib                  	       0x10684e988 JVM_WaitForReferencePendingList + 200
5   ???                           	       0x117068a88 ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x117060140 ???
9   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
10  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
11  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
12  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
13  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
14  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
15  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
16  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
17  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 12:: Java: Finalizer
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af86ac PlatformEvent::park() + 120
3   libjvm.dylib                  	       0x106ad6154 ObjectMonitor::wait(long, bool, JavaThread*) + 1340
4   libjvm.dylib                  	       0x106c94664 ObjectSynchronizer::wait(Handle, long, JavaThread*) + 292
5   libjvm.dylib                  	       0x106838b5c JVM_MonitorWait + 440
6   ???                           	       0x11783285c ???
7   ???                           	       0x11034b0c0 ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 13:: Java: Signal Dispatcher
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106c12a84 os::signal_wait() + 180
3   libjvm.dylib                  	       0x106aec09c signal_thread_entry(JavaThread*, JavaThread*) + 76
4   libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
5   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
6   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
7   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
8   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 14:: Java: Service Thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af93dc PlatformMonitor::wait(unsigned long long) + 144
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106b886e8 ServiceThread::service_thread_entry(JavaThread*, JavaThread*) + 520
5   libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 15:: Java: Monitor Deflation Thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106aab7d0 MonitorDeflationThread::monitor_deflation_thread_entry(JavaThread*, JavaThread*) + 252
5   libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 16:: Java: C2 CompilerThread0
0   libjvm.dylib                  	       0x10652be90 ConLNode::Opcode() const + 0
1   libjvm.dylib                  	       0x106ac9358 Node::destruct(PhaseValues*) + 468
2   libjvm.dylib                  	       0x106b28898 PhaseValues::uncached_makecon(Type const*) + 76
3   libjvm.dylib                  	       0x106b286b8 PhaseValues::makecon(Type const*) + 288
4   libjvm.dylib                  	       0x106b2ac2c PhaseIterGVN::transform_old(Node*) + 864
5   libjvm.dylib                  	       0x106b2a4ac PhaseIterGVN::optimize() + 180
6   libjvm.dylib                  	       0x106a0e61c PhaseMacroExpand::expand_macro_nodes() + 1736
7   libjvm.dylib                  	       0x106559990 Compile::Optimize() + 2872
8   libjvm.dylib                  	       0x1065580fc Compile::Compile(ciEnv*, ciMethod*, int, Options, DirectiveSet*) + 3220
9   libjvm.dylib                  	       0x1064af240 C2Compiler::compile_method(ciEnv*, ciMethod*, int, bool, DirectiveSet*) + 348
10  libjvm.dylib                  	       0x106568d40 CompileBroker::invoke_compiler_on_method(CompileTask*) + 1212
11  libjvm.dylib                  	       0x1065686c8 CompileBroker::compiler_thread_loop() + 1048
12  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
13  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
14  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
15  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
16  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 17:: Java: C1 CompilerThread0
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af945c PlatformMonitor::wait(unsigned long long) + 272
3   libjvm.dylib                  	       0x106ab6ed0 Monitor::wait(unsigned long long) + 124
4   libjvm.dylib                  	       0x106565064 CompileQueue::get(CompilerThread*) + 748
5   libjvm.dylib                  	       0x1065685a0 CompileBroker::compiler_thread_loop() + 752
6   libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
7   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
8   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
9   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
10  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 18:: Java: Notification Thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af93dc PlatformMonitor::wait(unsigned long long) + 144
3   libjvm.dylib                  	       0x106ab6e3c Monitor::wait_without_safepoint_check(unsigned long long) + 48
4   libjvm.dylib                  	       0x106acd5ec NotificationThread::notification_thread_entry(JavaThread*, JavaThread*) + 164
5   libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
6   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
7   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 19:: Java: Common-Cleaner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x110b0de0c ???
6   ???                           	       0x1170654fc ???
7   ???                           	       0x10ff3f120 ???
8   ???                           	       0x11706562c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x11706517c ???
11  ???                           	       0x117060140 ???
12  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
13  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
14  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
15  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
16  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
17  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
18  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
19  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
20  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 20:

Thread 21:

Thread 22:: com.apple.NSEventThread
0   libsystem_kernel.dylib        	       0x182262c34 mach_msg2_trap + 8
1   libsystem_kernel.dylib        	       0x182275028 mach_msg2_internal + 76
2   libsystem_kernel.dylib        	       0x18226b98c mach_msg_overwrite + 484
3   libsystem_kernel.dylib        	       0x182262fb4 mach_msg + 24
4   CoreFoundation                	       0x182344b90 __CFRunLoopServiceMachPort + 160
5   CoreFoundation                	       0x1823434e8 __CFRunLoopRun + 1188
6   CoreFoundation                	       0x1823fd35c _CFRunLoopRunSpecificWithOptions + 532
7   AppKit                        	       0x1867ddcb4 _NSEventThread + 184
8   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
9   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 23:: Java: AWT-Shutdown
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af86ac PlatformEvent::park() + 120
3   libjvm.dylib                  	       0x106ad6154 ObjectMonitor::wait(long, bool, JavaThread*) + 1340
4   libjvm.dylib                  	       0x106c94664 ObjectSynchronizer::wait(Handle, long, JavaThread*) + 292
5   libjvm.dylib                  	       0x106838b5c JVM_MonitorWait + 440
6   ???                           	       0x11783285c ???
7   ???                           	       0x11034b0c0 ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706562c ???
10  ???                           	       0x11706517c ???
11  ???                           	       0x117060140 ???
12  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
13  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
14  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
15  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
16  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
17  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
18  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
19  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
20  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 24:: Java: DefaultDispatcher-worker-1
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x110422a10 ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x117060140 ???
9   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
10  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
11  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
12  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
13  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
14  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
15  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
16  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
17  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 25:: Java: DefaultDispatcher-worker-2
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1108322d0 ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117060140 ???
8   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
9   libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
10  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
11  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
12  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
13  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
14  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
15  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
16  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 26:: Java: DefaultDispatcher-worker-3
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x110422a10 ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x117060140 ???
9   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
10  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
11  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
12  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
13  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
14  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
15  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
16  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
17  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 27:: Java: kotlinx.coroutines.DefaultExecutor
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706562c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117060140 ???
10  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
11  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
12  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
13  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
14  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
15  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
16  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
17  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
18  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 28:: Java: Reference Cleaner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x110b0de0c ???
6   ???                           	       0x1170654fc ???
7   ???                           	       0x10ff3f120 ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 29 Crashed:: Java: AWT-EventQueue-0
0   WebKit                        	       0x1abbc140c WebKit::runInitializationCode(void*) + 88
1   libc++.1.dylib                	       0x1821c04e4 std::__1::__call_once(unsigned long volatile&, void*, void (*)(void*)) + 196
2   WebKit                        	       0x1ab77516c WebKit::InitializeWebKit2() + 88
3   WebKit                        	       0x1ac029814 API::Object::Object() + 124
4   WebKit                        	       0x1abcd31e4 -[WKWebViewConfiguration init] + 108
5   libwebview.dylib              	       0x139d6b3e0 kfun:com.prof18.webview#createWebView(kotlinx.cinterop.CPointer<kotlinx.cinterop.CPointerVarOf<kotlinx.cinterop.CPointer<com.prof18.jni.JNINativeInterface_>>>;kotlinx.cinterop.CPointer<cnames.structs._jobject>){}kotlin.Int + 268 (WebViewHelper.kt:44)
6   libwebview.dylib              	       0x139d87980 Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_createWebView + 80
7   ???                           	       0x117068a88 ???
8   ???                           	       0x11706504c ???
9   ???                           	       0x11795e9d8 ???
10  ???                           	       0x11706517c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 30:: Java: GC Thread#1
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 31:: Java: GC Thread#2
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 32:: Java: GC Thread#3
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 33:: Java: GC Thread#4
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 34:: Java: GC Thread#5
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 35:: Java: Java2D Queue Flusher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af88d0 PlatformEvent::park_nanos(long) + 332
3   libjvm.dylib                  	       0x106ad6134 ObjectMonitor::wait(long, bool, JavaThread*) + 1308
4   libjvm.dylib                  	       0x106c94664 ObjectSynchronizer::wait(Handle, long, JavaThread*) + 292
5   libjvm.dylib                  	       0x106838b5c JVM_MonitorWait + 440
6   ???                           	       0x11783285c ???
7   ???                           	       0x11034b0c0 ???
8   ???                           	       0x11706562c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 36:: Java: Java2D Disposer
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ce0 Parker::park(bool, long) + 512
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x110c199e8 ???
6   ???                           	       0x110c5ebb4 ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x11706562c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 37:: Java: TimerQueue
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ce0 Parker::park(bool, long) + 512
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x110c199e8 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x11706562c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117060140 ???
10  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
11  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
12  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
13  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
14  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
15  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
16  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
17  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
18  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 38:: Java: GC Thread#6
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 39:: Java: GC Thread#7
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 40:: Java: G1 Conc#1
0   libsystem_kernel.dylib        	       0x182262bb0 semaphore_wait_trap + 8
1   libjvm.dylib                  	       0x106b865a4 OSXSemaphore::wait() + 24
2   libjvm.dylib                  	       0x106d79830 WorkerThread::run() + 84
3   libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
4   libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 41:

Thread 42:: Java: pool-1-thread-1
0   libCGInterfaces.dylib         	       0x1b4d440f0 vImageConverter_Finalize + 36
1   CoreFoundation                	       0x1823ff078 _CFRelease + 296
2   CoreGraphics                  	       0x18904cffc provider_for_destination_release_info + 76
3   CoreGraphics                  	       0x18904cec0 data_provider_finalize + 64
4   CoreGraphics                  	       0x189015080 data_provider_retain_count + 96
5   CoreFoundation                	       0x1823ff484 _CFRelease + 1332
6   CoreGraphics                  	       0x18904926c img_data_lock + 8044
7   CoreGraphics                  	       0x189042718 CGSImageDataLock + 1168
8   CoreGraphics                  	       0x189041e8c ripc_AcquireRIPImageData + 1420
9   CoreGraphics                  	       0x1890404e8 ripc_DrawImage + 808
10  CoreGraphics                  	       0x189040028 CGContextDrawImageWithOptions + 1032
11  CoreGraphics                  	       0x18903fb60 CGContextDrawImage + 556
12  CoreText                      	       0x18521eed4 (anonymous namespace)::TCGImageData::DrawAtPoint(CGContext*, CGPoint) const + 240
13  CoreText                      	       0x185225240 DrawGlyphsAtPositions(TFont const*, unsigned short const*, CGPoint const*, unsigned long, CGContext*, (anonymous namespace)::DrawGlyphsConfiguration const&)::$_1::operator()(CGAffineTransform, CGAffineTransform) const + 292
14  CoreText                      	       0x18521fa58 DrawGlyphsAtPositions(TFont const*, unsigned short const*, CGPoint const*, unsigned long, CGContext*, (anonymous namespace)::DrawGlyphsConfiguration const&) + 2496
15  CoreText                      	       0x1851c3ee0 CTFontDrawGlyphs + 248
16  libskiko-macos-arm64.dylib    	       0x12c699ca0 SkScalerContext_Mac::Offscreen::getCG(SkScalerContext_Mac const&, SkGlyph const&, unsigned short, unsigned long*, bool) + 1624
17  libskiko-macos-arm64.dylib    	       0x12c699f18 SkScalerContext_Mac::generateImage(SkGlyph const&, void*) + 84
18  libskiko-macos-arm64.dylib    	       0x12c5971a4 SkScalerContext::getImage(SkGlyph const&) + 576
19  libskiko-macos-arm64.dylib    	       0x12c520798 SkGlyph::setImage(SkArenaAlloc*, SkScalerContext*) + 92
20  libskiko-macos-arm64.dylib    	       0x12c5a6380 SkStrike::prepareImages(SkSpan<SkPackedGlyphID const>, SkGlyph const**) + 148
21  libskiko-macos-arm64.dylib    	       0x12c5a8d34 SkBulkGlyphMetricsAndImages::glyph(SkPackedGlyphID) + 88
22  libskiko-macos-arm64.dylib    	       0x12c78d914 sktext::gpu::GlyphVector::regenerateAtlasForGanesh(int, int, skgpu::MaskFormat, int, GrMeshDrawTarget*) + 440
23  libskiko-macos-arm64.dylib    	       0x12c7f6c54 non-virtual thunk to (anonymous namespace)::DirectMaskSubRun::regenerateAtlas(int, int, std::__1::function<std::__1::tuple<bool, int> (sktext::gpu::GlyphVector*, int, int, skgpu::MaskFormat, int)>) const + 76
24  libskiko-macos-arm64.dylib    	       0x12c7451f0 skgpu::ganesh::AtlasTextOp::onPrepareDraws(GrMeshDrawTarget*) + 1428
25  libskiko-macos-arm64.dylib    	       0x12c765e6c skgpu::ganesh::OpsTask::onPrepare(GrOpFlushState*) + 460
26  libskiko-macos-arm64.dylib    	       0x12c6dde78 GrRenderTask::prepare(GrOpFlushState*) + 104
27  libskiko-macos-arm64.dylib    	       0x12c6c32b4 GrDrawingManager::executeRenderTasks(GrOpFlushState*) + 92
28  libskiko-macos-arm64.dylib    	       0x12c6c2d1c GrDrawingManager::flush(SkSpan<GrSurfaceProxy*>, SkSurfaces::BackendSurfaceAccess, GrFlushInfo const&, skgpu::MutableTextureState const*) + 1284
29  libskiko-macos-arm64.dylib    	       0x12c6c3810 GrDrawingManager::flushSurfaces(SkSpan<GrSurfaceProxy*>, SkSurfaces::BackendSurfaceAccess, GrFlushInfo const&, skgpu::MutableTextureState const*) + 168
30  libskiko-macos-arm64.dylib    	       0x12c4acfa0 Java_org_jetbrains_skia_DirectContextKt_DirectContext_1nFlushDefault + 36
31  ???                           	       0x117068a88 ???
32  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 9
33  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 8
34  ???                           	       0x117064f70 ???
35  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 7
--------
-------- ELIDED 3 LEVELS OF RECURSION THROUGH 0x11706517c ???
--------
39  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 3
40  ???                           	       0x11795e9d8 ???
41  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 2
42  ???                           	       0x11706562c ???
43  ???                           	       0x11706517c ???
-------- RECURSION LEVEL 1
44  ???                           	       0x117060140 ???
45  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
46  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
47  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
48  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
49  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
50  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
51  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
52  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
53  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 43:: Java: Timer-0
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af88d0 PlatformEvent::park_nanos(long) + 332
3   libjvm.dylib                  	       0x106ad6134 ObjectMonitor::wait(long, bool, JavaThread*) + 1308
4   libjvm.dylib                  	       0x106c94664 ObjectSynchronizer::wait(Handle, long, JavaThread*) + 292
5   libjvm.dylib                  	       0x106838b5c JVM_MonitorWait + 440
6   ???                           	       0x117068a88 ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 44:: CVDisplayLink
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a6108 _pthread_cond_wait + 1028
2   CoreVideo                     	       0x18c8b5b3c CVDisplayLink::waitUntil(unsigned long long) + 336
3   CoreVideo                     	       0x18c8b4c24 CVDisplayLink::runIOThread() + 500
4   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
5   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 45:: Java: DefaultDispatcher-worker-4
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 46:: Java: DefaultDispatcher-worker-6
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 47:: Java: DefaultDispatcher-worker-5
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 48:: Java: DefaultDispatcher-worker-7
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1108322d0 ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117060140 ???
8   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
9   libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
10  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
11  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
12  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
13  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
14  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
15  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
16  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 49:: Java: DefaultDispatcher-worker-8
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 50:: Java: DefaultDispatcher-worker-11
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1108322d0 ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117060140 ???
8   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
9   libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
10  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
11  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
12  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
13  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
14  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
15  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
16  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 51:: Java: DefaultDispatcher-worker-10
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 52:: Java: DefaultDispatcher-worker-9
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 53:: Java: DefaultDispatcher-worker-12
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x110422a10 ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x117060140 ???
9   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
10  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
11  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
12  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
13  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
14  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
15  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
16  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
17  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 54:: Java: DefaultDispatcher-worker-13
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1108322d0 ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117060140 ???
8   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
9   libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
10  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
11  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
12  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
13  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
14  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
15  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
16  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 55:: Java: DefaultDispatcher-worker-14
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 56:: Java: DefaultDispatcher-worker-15
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 57:: Java: DefaultDispatcher-worker-16
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706517c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x117060140 ???
11  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
12  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
13  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
14  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
15  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
16  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
17  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
18  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
19  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 58:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 59:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 60:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117065420 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x11706517c ???
11  ???                           	       0x11706562c ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x117060140 ???
14  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
15  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
16  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
17  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
18  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
19  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
20  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
21  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
22  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 61:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 62:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 63:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 64:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 65:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 66:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 67:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 68:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 69:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 70:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 71:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 72:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117065420 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x11706517c ???
11  ???                           	       0x11706562c ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x117060140 ???
14  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
15  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
16  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
17  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
18  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
19  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
20  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
21  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
22  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 73:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 74:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af88d0 PlatformEvent::park_nanos(long) + 332
3   libjvm.dylib                  	       0x106ad6134 ObjectMonitor::wait(long, bool, JavaThread*) + 1308
4   libjvm.dylib                  	       0x106c94664 ObjectSynchronizer::wait(Handle, long, JavaThread*) + 292
5   libjvm.dylib                  	       0x106838b5c JVM_MonitorWait + 440
6   ???                           	       0x11783285c ???
7   ???                           	       0x11034b0c0 ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x11706562c ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706562c ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 75:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 76:: Java: OkHttp 9to5linux.com
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11057c504 ???
5   ???                           	       0x1105623dc ???
6   ???                           	      0x6e00610069 ???

Thread 77:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 78:: Java: OkHttp www.basketuniverso.it
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11057c504 ???
5   ???                           	       0x1105623dc ???
6   ???                           	         0x147e208 ???

Thread 79:: Java: OkHttp www.macstories.net
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11057c504 ???
5   ???                           	         0x147e208 ???

Thread 80:: Java: OkHttp photos5.appleinsider.com
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11057c504 ???
5   ???                           	         0x147e208 ???

Thread 81:: Java: OkHttp www.pianetabasket.com
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x110ba994c ???
4   ???                           	         0x147e208 ???

Thread 82:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 83:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 84:: Java: OkHttp berlinomagazine.com
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11057c504 ???
5   ???                           	       0x1105623dc ???
6   ???                           	      0x650076006f ???

Thread 85:: Java: Okio Watchdog
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x110b0de0c ???
6   ???                           	       0x1170654fc ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117060140 ???
9   libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
10  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
11  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
12  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
13  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
14  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
15  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
16  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
17  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 86:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706562c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117060140 ???
10  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
11  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
12  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
13  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
14  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
15  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
16  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
17  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
18  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 87:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706562c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117060140 ???
10  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
11  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
12  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
13  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
14  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
15  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
16  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
17  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
18  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 88:: Java: OkHttp sifted.eu
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x11706504c ???
4   ???                           	       0x11706517c ???
5   ???                           	       0x11030e778 ???
6   ???                           	       0x11706504c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x11706504c ???
9   ???                           	       0x117866164 ???
10  ???                           	         0x147e208 ???

Thread 89:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 90:: Java: OkHttp net-storage.tcccdn.com
0   libsystem_kernel.dylib        	       0x18226b6c0 poll + 8
1   libnio.dylib                  	       0x10079a39c Java_sun_nio_ch_Net_poll + 80
2   ???                           	       0x1178639f8 ???
3   ???                           	       0x110ba994c ???
4   ???                           	         0x147e208 ???

Thread 91:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 92:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 93:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 94:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x1104dec38 ???
6   ???                           	       0x117064f70 ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117065420 ???
10  ???                           	       0x117064f70 ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x11706562c ???
13  ???                           	       0x11706517c ???
14  ???                           	       0x117060140 ???
15  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
16  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
17  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
18  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
19  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
20  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
21  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
22  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
23  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 95:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 96:: Java: OkHttp TaskRunner
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x117068a88 ???
5   ???                           	       0x11706517c ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x117064f70 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x117064f70 ???
10  ???                           	       0x117065420 ???
11  ???                           	       0x117064f70 ???
12  ???                           	       0x11706517c ???
13  ???                           	       0x11706562c ???
14  ???                           	       0x11706517c ???
15  ???                           	       0x117060140 ???
16  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
17  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
18  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
19  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
20  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
21  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
22  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
23  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
24  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 97:

Thread 98:: CVDisplayLink
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a6108 _pthread_cond_wait + 1028
2   CoreVideo                     	       0x18c8b5b3c CVDisplayLink::waitUntil(unsigned long long) + 336
3   CoreVideo                     	       0x18c8b4c24 CVDisplayLink::runIOThread() + 500
4   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
5   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 99:: CVDisplayLink
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a6108 _pthread_cond_wait + 1028
2   CoreVideo                     	       0x18c8b5b3c CVDisplayLink::waitUntil(unsigned long long) + 336
3   CoreVideo                     	       0x18c8b4c24 CVDisplayLink::runIOThread() + 500
4   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
5   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 100:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x110ac5024 ???
7   ???                           	       0x117065420 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x11706562c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 101:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x110ac5024 ???
7   ???                           	       0x117065420 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x11706562c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 102:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x110ac5024 ???
7   ???                           	       0x117065420 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x11706562c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 103:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x110ac5024 ???
7   ???                           	       0x117065420 ???
8   ???                           	       0x117064f70 ???
9   ???                           	       0x11706517c ???
10  ???                           	       0x11706562c ???
11  ???                           	       0x11706517c ???
12  ???                           	       0x117060140 ???
13  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
14  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
15  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
16  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
17  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
18  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
19  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
20  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
21  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 104:: Java: OkHttp Dispatcher
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libjvm.dylib                  	       0x106af8ccc Parker::park(bool, long) + 492
3   libjvm.dylib                  	       0x106cfa5d4 Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long) + 328
4   ???                           	       0x11783355c ???
5   ???                           	       0x11077b8cc ???
6   ???                           	       0x11706517c ???
7   ???                           	       0x11706562c ???
8   ???                           	       0x11706517c ???
9   ???                           	       0x117060140 ???
10  libjvm.dylib                  	       0x10677a584 JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*) + 992
11  libjvm.dylib                  	       0x1067794b4 JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*) + 320
12  libjvm.dylib                  	       0x106779580 JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*) + 100
13  libjvm.dylib                  	       0x10684c584 thread_entry(JavaThread*, JavaThread*) + 156
14  libjvm.dylib                  	       0x10678e8e8 JavaThread::thread_main_inner() + 152
15  libjvm.dylib                  	       0x106cc4244 Thread::call_run() + 200
16  libjvm.dylib                  	       0x106af0f90 thread_native_entry(Thread*) + 280
17  libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
18  libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 105:: GC Timer thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libc++.1.dylib                	       0x1821d67e4 std::__1::condition_variable::__do_timed_wait(std::__1::unique_lock<std::__1::mutex>&, std::__1::chrono::time_point<std::__1::chrono::system_clock, std::__1::chrono::duration<long long, std::__1::ratio<1l, 1000000000l>>>) + 104
3   libwebview.dylib              	       0x139d85ddc void kotlin::RepeatedTimer<kotlin::steady_clock>::Run<kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()>(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) + 256
4   libwebview.dylib              	       0x139d8616c void* std::__1::__thread_proxy[abi:ne200100]<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct>>, void (*)(kotlin::ScopedThread::attributes, void (*&&)(void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), kotlin::ScopedThread::attributes, void (*)(void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), void (kotlin::RepeatedTimer<kotlin::steady_clock>::*)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()>>(void*) + 124
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 106:: Main GC thread
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   libc++.1.dylib                	       0x1821d674c std::__1::condition_variable::wait(std::__1::unique_lock<std::__1::mutex>&) + 32
3   libwebview.dylib              	       0x139d81694 std::__1::invoke_result<kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>::type kotlin::UtilityThread::Run<kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&) + 212
4   libwebview.dylib              	       0x139d817d4 void* std::__1::__thread_proxy[abi:ne200100]<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct>>, void (*)(kotlin::ScopedThread::attributes, void (*&&)(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::ScopedThread::attributes, void (*)(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>>(void*) + 116
5   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
6   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8

Thread 107:: JavaScriptCore libpas scavenger
0   libsystem_kernel.dylib        	       0x1822664f8 __psynch_cvwait + 8
1   libsystem_pthread.dylib       	       0x1822a60dc _pthread_cond_wait + 984
2   JavaScriptCore                	       0x1a4f67e78 scavenger_thread_main + 1440
3   libsystem_pthread.dylib       	       0x1822a5c08 _pthread_start + 136
4   libsystem_pthread.dylib       	       0x1822a0ba8 thread_start + 8


Thread 29 crashed with ARM Thread State (64-bit):
x0: 0x000000000000003b   x1: 0x00000001ac90fbbf   x2: 0x00000001ac90fc6c   x3: 0x0000000000000026
x4: 0x0000000000000000   x5: 0x0000000000000000   x6: 0x0000000000000061   x7: 0x0000000000000012
x8: 0x0000000172e5b000   x9: 0x00000001eec72080  x10: 0x0000000b8afb19c3  x11: 0x0000000000000003
x12: 0x0000000000000003  x13: 0x0000000b98a582a0  x14: 0x00000001eeca2310  x15: 0x00000001eeca2310
x16: 0x00000001822a16e4  x17: 0x00000001f026f8f0  x18: 0x0000000000000000  x19: 0x00000001eef845b0
x20: 0x00000001abbc1284  x21: 0x0000000172e59aa0  x22: 0x0000000000000000  x23: 0x00000004d64e7618
x24: 0x0000000172e59e68  x25: 0x00000004db75a350  x26: 0x0000000172e59b90  x27: 0x00000004d9ecc948
x28: 0x0000000b98032300   fp: 0x0000000172e59a30   lr: 0x00000001abbc13cc
sp: 0x0000000172e59a30   pc: 0x00000001abbc140c cpsr: 0x80001000
far: 0x0000000000000000  esr: 0xf200c471 (Breakpoint) pointer authentication trap IB

Binary Images:
0x100404000 -        0x100407fff java (*) <4acdb266-eb95-3273-bb57-c2e40a559c3e> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/bin/java
0x100444000 -        0x100457fff libjli.dylib (*) <d08d7a99-f8f9-34f3-aea6-58bd8b3d1f99> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libjli.dylib
0x1062c0000 -        0x106ee3fff libjvm.dylib (*) <8740592f-ba05-3ed9-8468-4ee832bedc5e> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/server/libjvm.dylib
0x10046c000 -        0x10046ffff libjimage.dylib (*) <0383fb59-81f4-3bbf-9741-c22701a96b4b> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libjimage.dylib
0x1004f0000 -        0x100503fff libjava.dylib (*) <50a1bc12-5947-33f5-86d2-c745235440c1> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libjava.dylib
0x100794000 -        0x10079ffff libnio.dylib (*) <ab4bb1dd-c841-3698-8d60-0188a71b9c00> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libnio.dylib
0x1007b4000 -        0x1007bffff libnet.dylib (*) <8449cb27-edb4-34ae-816e-e7d249c44f4e> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libnet.dylib
0x1007f8000 -        0x10080ffff libzip.dylib (*) <c38326d7-e933-390a-b6fa-57038e37c0ee> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libzip.dylib
0x12c488000 -        0x12d5cffff libskiko-macos-arm64.dylib (*) <06e3c1c6-f227-3754-b778-2700d14f03df> /Users/USER/*/libskiko-macos-arm64.dylib
0x1061ec000 -        0x10624ffff libawt.dylib (*) <8199d7eb-a961-3e8b-b598-76fd218d6619> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libawt.dylib
0x124b2c000 -        0x124b8ffff libmlib_image.dylib (*) <83560f65-7886-32da-8d93-9966a4742be2> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libmlib_image.dylib
0x124ccc000 -        0x124d83fff libawt_lwawt.dylib (*) <087fdeb9-bd42-33c7-91f7-e58eee343ab0> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libawt_lwawt.dylib
0x106160000 -        0x106167fff libosxapp.dylib (*) <7ff3a0f1-7030-3932-bc0b-b6607c38df4a> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libosxapp.dylib
0x12b2b8000 -        0x12b45bfff libfontmanager.dylib (*) <35779de6-1933-3b15-9725-f0a8b04e181d> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libfontmanager.dylib
0x124dfc000 -        0x124e77fff libfreetype.dylib (*) <57965598-6495-31f9-9f38-4bdf1a5a0885> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libfreetype.dylib
0x1061b4000 -        0x1061bffff libobjc-trampolines.dylib (*) <f8bd9069-8c4f-37ea-af9a-2b1060f54e4f> /usr/lib/libobjc-trampolines.dylib
0x13ac30000 -        0x13b37ffff com.apple.AGXMetalG13X (341.11) <f4b41620-c555-31a5-ae52-eca9b80bf0d8> /System/Library/Extensions/AGXMetalG13X.bundle/Contents/MacOS/AGXMetalG13X
0x12b294000 -        0x12b29bfff libosxui.dylib (*) <58115166-58e0-3807-b591-807407765146> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libosxui.dylib
0x139bb8000 -        0x139c0ffff libikloud.dylib (*) <3d70fa62-b878-3d61-818c-c887c8a1436d> /Users/USER/*/libikloud.dylib
0x139d4c000 -        0x139dc3fff libwebview.dylib (*) <68b3c35d-923b-3973-b3f0-1062814470f3> /Users/USER/*/libwebview.dylib
0x139ce0000 -        0x139ce3fff libjawt.dylib (*) <2bc56aa7-41f2-38e0-8c74-f6af7fb3bca7> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libjawt.dylib
0x139d18000 -        0x139d33fff jna3417252500363705797.tmp (*) <624d6700-8917-3027-87d7-871a6cd08ed5> /Users/USER/Library/Caches/*/jna3417252500363705797.tmp
0x13abe4000 -        0x13abe7fff libprefs.dylib (*) <1e9150d9-86d6-3570-ba22-f90b792a62cc> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libprefs.dylib
0x13c5e4000 -        0x13c6dffff sqlite-3.49.1.0-25948a9c-20ed-4cd2-b495-bb93b6d013c1-libsqlitejdbc.dylib (*) <86cd20e1-38ec-32e5-b772-8f07d8f09077> /private/var/folders/*/sqlite-3.49.1.0-25948a9c-20ed-4cd2-b495-bb93b6d013c1-libsqlitejdbc.dylib
0x13c578000 -        0x13c57bfff libextnet.dylib (*) <62c7d081-4966-3c2c-9fea-caa4c8a66bbe> /Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home/lib/libextnet.dylib
0x182262000 -        0x18229e49f libsystem_kernel.dylib (*) <9fe7c84d-0c2b-363f-bee5-6fd76d67a227> /usr/lib/system/libsystem_kernel.dylib
0x1822e5000 -        0x18282babf com.apple.CoreFoundation (6.9) <3c4a3add-9e48-33da-82ee-80520e6cbe3b> /System/Library/Frameworks/CoreFoundation.framework/Versions/A/CoreFoundation
0x18ed3f000 -        0x18f0417ff com.apple.HIToolbox (2.1.1) <9ab64c08-0685-3a0d-9a7e-83e7a1e9ebb4> /System/Library/Frameworks/Carbon.framework/Versions/A/Frameworks/HIToolbox.framework/Versions/A/HIToolbox
0x18672e000 -        0x187e5ab9f com.apple.AppKit (6.9) <3c0949bb-e361-369a-80b7-17440eb09e98> /System/Library/Frameworks/AppKit.framework/Versions/C/AppKit
0x183b2f000 -        0x184ad225f com.apple.Foundation (6.9) <00467f1f-216a-36fe-8587-c820c7e0437d> /System/Library/Frameworks/Foundation.framework/Versions/C/Foundation
0x181ed5000 -        0x181f73f63 dyld (*) <b50f5a1a-be81-3068-92e1-3554f2be478a> /usr/lib/dyld
0x0 - 0xffffffffffffffff ??? (*) <00000000-0000-0000-0000-000000000000> ???
0x18229f000 -        0x1822ababb libsystem_pthread.dylib (*) <e95973b8-824c-361e-adf4-390747c40897> /usr/lib/system/libsystem_pthread.dylib
0x181ea4000 -        0x181ed4bcb libdyld.dylib (*) <a64128d9-375b-36ae-8feb-69d80f987d3c> /usr/lib/system/libdyld.dylib
0x1ab758000 -        0x1acc728bf com.apple.WebKit (21622) <3b55482a-efe2-35a7-b1c9-3f41a823a30b> /System/Library/Frameworks/WebKit.framework/Versions/A/WebKit
0x1821b4000 -        0x182246e53 libc++.1.dylib (*) <b29f2164-26b0-3016-a871-82de5a4637ff> /usr/lib/libc++.1.dylib
0x1b4d41000 -        0x1b4d5bad3 libCGInterfaces.dylib (*) <b8c7c1aa-5203-3bb5-8760-a7870b239d36> /System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vImage.framework/Versions/A/Libraries/libCGInterfaces.dylib
0x189009000 -        0x18975de1f com.apple.CoreGraphics (2.0) <a5317723-cc87-3367-b3ae-fd7b0ea01333> /System/Library/Frameworks/CoreGraphics.framework/Versions/A/CoreGraphics
0x18517f000 -        0x1853ab65f com.apple.CoreText (877.1.0.5) <db7e0880-8f10-3512-b6ef-3c35511ce208> /System/Library/Frameworks/CoreText.framework/Versions/A/CoreText
0x18bbb2000 -        0x18bfe8277 com.apple.vImage (8.1) <90793c47-d770-3ead-a12a-b1696a2b16b8> /System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vImage.framework/Versions/A/vImage
0x18c8b2000 -        0x18c935fff com.apple.CoreVideo (1.8) <d8605842-8c6c-36d7-820d-2132d91e0c06> /System/Library/Frameworks/CoreVideo.framework/Versions/A/CoreVideo
0x1a366a000 -        0x1a5188e7f com.apple.JavaScriptCore (21622) <c79071c9-db50-3264-a316-94abd0d3b9a9> /System/Library/Frameworks/JavaScriptCore.framework/Versions/A/JavaScriptCore

External Modification Summary:
Calls made by other processes targeting this process:
task_for_pid: 0
thread_create: 0
thread_set_state: 0
Calls made by this process:
task_for_pid: 0
thread_create: 0
thread_set_state: 0
Calls made by all processes on this machine:
task_for_pid: 0
thread_create: 0
thread_set_state: 0

-----------
Full Report
-----------

{"app_name":"java","timestamp":"2025-12-30 15:49:18.00 +0100","app_version":"21.0.4","slice_uuid":"4acdb266-eb95-3273-bb57-c2e40a559c3e","build_version":"7","platform":1,"share_with_app_devs":0,"is_first_party":1,"bug_type":"309","os_version":"macOS 26.1 (25B78)","roots_installed":0,"incident_id":"17EF0077-0278-4F40-B761-D18414904758","name":"java"}
{
"uptime" : 13000,
"procRole" : "Foreground",
"version" : 2,
"userID" : 501,
"deployVersion" : 210,
"modelCode" : "MacBookPro18,3",
"coalitionID" : 1338,
"osVersion" : {
"train" : "macOS 26.1",
"build" : "25B78",
"releaseType" : "User"
},
"captureTime" : "2025-12-30 15:49:09.0339 +0100",
"codeSigningMonitor" : 1,
"incident" : "17EF0077-0278-4F40-B761-D18414904758",
"pid" : 37880,
"translated" : false,
"cpuType" : "ARM-64",
"roots_installed" : 0,
"bug_type" : "309",
"procLaunch" : "2025-12-30 15:48:55.4988 +0100",
"procStartAbsTime" : 333593342952,
"procExitAbsTime" : 333916747100,
"procName" : "java",
"procPath" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/bin\/java",
"bundleInfo" : {"CFBundleVersion":"7","CFBundleShortVersionString":"21.0.4"},
"parentProc" : "java",
"parentPid" : 37371,
"coalitionName" : "com.google.antigravity",
"crashReporterKey" : "328A6717-B4DA-BF04-BD7F-76EAA080F4D0",
"appleIntelligenceStatus" : {"state":"unavailable","reasons":["notOptedIn","siriAssetIsNotReady","assetIsNotReady"]},
"developerMode" : 1,
"responsiblePid" : 3219,
"responsibleProc" : "Electron",
"codeSigningID" : "com.azul.zulu.java",
"codeSigningTeamID" : "TDTHCUPYFR",
"codeSigningFlags" : 570491649,
"codeSigningValidationCategory" : 6,
"codeSigningTrustLevel" : 4294967295,
"codeSigningAuxiliaryInfo" : 0,
"instructionByteStream" : {"beforePC":"0AceylAA8LYgjjjUMMYzFGFqANAh\/C6RYmoA0EKwMZFgB4BSwwSAUg==","atPC":"II441CAAINTAA1\/WVp8zFOb\/\/xd\/IwPV\/Xu\/qf0DAJFgagDQAFwykQ=="},
"bootSessionUUID" : "30C26270-E10D-4168-BF97-FF94985FEBE3",
"wakeTime" : 1074,
"sleepWakeUUID" : "85693A8E-4302-4AA6-9A51-C19FB9429057",
"sip" : "enabled",
"exception" : {"codes":"0x0000000000000001, 0x00000001abbc140c","rawCodes":[1,7176197132],"type":"EXC_BREAKPOINT","signal":"SIGTRAP"},
"termination" : {"flags":0,"code":5,"namespace":"SIGNAL","indicator":"Trace\/BPT trap: 5","byProc":"exc handler","byPid":37880},
"os_fault" : {"process":"java"},
"extMods" : {"caller":{"thread_create":0,"thread_set_state":0,"task_for_pid":0},"system":{"thread_create":0,"thread_set_state":0,"task_for_pid":0},"targeted":{"thread_create":0,"thread_set_state":0,"task_for_pid":0},"warnings":0},
"faultingThread" : 29,
"threads" : [{"id":501615,"threadState":{"x":[{"value":268451845},{"value":21592279046},{"value":8589934592,"objc-selector":""},{"value":30799210479616},{"value":0},{"value":30799210479616},{"value":2},{"value":4294967295},{"value":0},{"value":17179869184},{"value":0},{"value":2},{"value":0},{"value":0},{"value":7171},{"value":0},{"value":18446744073709551569},{"value":8324045552},{"value":0},{"value":4294967295},{"value":2},{"value":30799210479616},{"value":0},{"value":30799210479616},{"value":6167656328},{"value":8589934592,"objc-selector":""},{"value":21592279046},{"value":18446744073709550527},{"value":4412409862,"symbolLocation":82,"symbol":"CRSCommandListenerThread::_buffer"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478581800},"cpsr":{"value":4096},"fp":{"value":6167656176},"sp":{"value":6167656096},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478507060},"far":{"value":0}},"queue":"com.apple.main-thread","frames":[{"imageOffset":3124,"symbol":"mach_msg2_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":77864,"symbol":"mach_msg2_internal","symbolLocation":76,"imageIndex":25},{"imageOffset":39308,"symbol":"mach_msg_overwrite","symbolLocation":484,"imageIndex":25},{"imageOffset":4020,"symbol":"mach_msg","symbolLocation":24,"imageIndex":25},{"imageOffset":392080,"symbol":"__CFRunLoopServiceMachPort","symbolLocation":160,"imageIndex":26},{"imageOffset":386280,"symbol":"__CFRunLoopRun","symbolLocation":1188,"imageIndex":26},{"imageOffset":1147740,"symbol":"_CFRunLoopRunSpecificWithOptions","symbolLocation":532,"imageIndex":26},{"imageOffset":792424,"symbol":"RunCurrentEventLoopInMode","symbolLocation":316,"imageIndex":27},{"imageOffset":805520,"symbol":"ReceiveNextEventCommon","symbolLocation":488,"imageIndex":27},{"imageOffset":2417416,"symbol":"_BlockUntilNextEventMatchingListInMode","symbolLocation":48,"imageIndex":27},{"imageOffset":5399488,"symbol":"_DPSBlockUntilNextEventMatchingListInMode","symbolLocation":236,"imageIndex":28},{"imageOffset":130612,"symbol":"_DPSNextEvent","symbolLocation":588,"imageIndex":28},{"imageOffset":11460420,"symbol":"-[NSApplication(NSEventRouting) _nextEventMatchingEventMask:untilDate:inMode:dequeue:]","symbolLocation":688,"imageIndex":28},{"imageOffset":11459664,"symbol":"-[NSApplication(NSEventRouting) nextEventMatchingMask:untilDate:inMode:dequeue:]","symbolLocation":72,"imageIndex":28},{"imageOffset":14152,"symbol":"-[NSApplicationAWT nextEventMatchingMask:untilDate:inMode:dequeue:]","symbolLocation":136,"imageIndex":12},{"imageOffset":100224,"symbol":"-[NSApplication run]","symbolLocation":368,"imageIndex":28},{"imageOffset":13672,"symbol":"+[NSApplicationAWT runAWTLoopWithApp:]","symbolLocation":132,"imageIndex":12},{"imageOffset":406572,"symbol":"+[AWTStarter starter:headless:]","symbolLocation":376,"imageIndex":11},{"imageOffset":19848,"symbol":"+[ThreadUtilities invokeBlockCopy:]","symbolLocation":28,"imageIndex":12},{"imageOffset":229652,"symbol":"__NSThreadPerformPerform","symbolLocation":264,"imageIndex":29},{"imageOffset":391656,"symbol":"__CFRUNLOOP_IS_CALLING_OUT_TO_A_SOURCE0_PERFORM_FUNCTION__","symbolLocation":28,"imageIndex":26},{"imageOffset":391548,"symbol":"__CFRunLoopDoSource0","symbolLocation":172,"imageIndex":26},{"imageOffset":390888,"symbol":"__CFRunLoopDoSources0","symbolLocation":232,"imageIndex":26},{"imageOffset":385912,"symbol":"__CFRunLoopRun","symbolLocation":820,"imageIndex":26},{"imageOffset":1147740,"symbol":"_CFRunLoopRunSpecificWithOptions","symbolLocation":532,"imageIndex":26},{"imageOffset":52616,"symbol":"CreateExecutionEnvironment","symbolLocation":404,"imageIndex":1},{"imageOffset":33784,"symbol":"JLI_Launch","symbolLocation":1152,"imageIndex":1},{"imageOffset":15288,"symbol":"main","symbolLocation":404,"imageIndex":0},{"imageOffset":36180,"symbol":"start","symbolLocation":7184,"imageIndex":30}]},{"id":501620,"frames":[{"imageOffset":10980,"symbol":"__ulock_wait","symbolLocation":8,"imageIndex":25},{"imageOffset":37036,"symbol":"_pthread_join","symbolLocation":608,"imageIndex":32},{"imageOffset":54380,"symbol":"CallJavaMainInNewThread","symbolLocation":184,"imageIndex":1},{"imageOffset":48848,"symbol":"ContinueInNewThread","symbolLocation":148,"imageIndex":1},{"imageOffset":39152,"symbol":"JLI_Launch","symbolLocation":6520,"imageIndex":1},{"imageOffset":15288,"symbol":"main","symbolLocation":404,"imageIndex":0},{"imageOffset":56648,"symbol":"apple_main","symbolLocation":88,"imageIndex":1},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}],"threadState":{"x":[{"value":18446744073709551612},{"value":0},{"value":5379},{"value":0},{"value":150997247},{"value":3},{"value":0},{"value":0},{"value":5379},{"value":16908290},{"value":17},{"value":0},{"value":8301042544,"symbolLocation":0,"symbol":"vm_page_size"},{"value":3916},{"value":1152921504606846976},{"value":18446744072631617535},{"value":515},{"value":8324043904},{"value":0},{"value":6170390528},{"value":2},{"value":6170390580},{"value":16908290},{"value":6168260832},{"value":8301052012,"symbolLocation":0,"symbol":"_pthread_list_lock"},{"value":17},{"value":4309072352},{"value":0},{"value":4309075112}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478790828},"cpsr":{"value":1073745920},"fp":{"value":6168253696},"sp":{"value":6168253600},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478514916},"far":{"value":0}}},{"id":501621,"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":5445080,"symbol":"jni_invoke_static(JNIEnv_*, JavaValue*, _jobject*, JNICallType, _jmethodID*, JNI_ArgumentPusher*, JavaThread*)","symbolLocation":380,"imageIndex":2},{"imageOffset":5459924,"symbol":"jni_CallStaticVoidMethod","symbolLocation":276,"imageIndex":2},{"imageOffset":42724,"symbol":"JavaMain","symbolLocation":2320,"imageIndex":1},{"imageOffset":54452,"symbol":"ThreadJavaMain","symbolLocation":12,"imageIndex":1},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}],"threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":99999999},{"value":435945000},{"value":6170387512},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":4309116736},{"value":4309116800},{"value":6170390752},{"value":435945000},{"value":99999999},{"value":0},{"value":1},{"value":256},{"value":0},{"value":4309115232}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6170387632},"sp":{"value":6170387488},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}}},{"id":501622,"name":"Java: GC Thread#0","threadState":{"x":[{"value":14},{"value":32},{"value":2},{"value":68719460488},{"value":6172535695},{"value":0},{"value":18446744072631617535},{"value":18446726482597246976},{"value":6172536504},{"value":4309154016},{"value":4294967295},{"value":72},{"value":4302299968},{"value":5295247999370240},{"value":2048},{"value":11},{"value":18446744073709551580},{"value":4309155552},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":2},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6172536480},"sp":{"value":6172536464},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501623,"name":"Java: G1 Main Marker","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6174682344},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":4309103328},{"value":4309103408},{"value":6174683360},{"value":0},{"value":0},{"value":768},{"value":769},{"value":1024},{"value":6174682624},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6174682464},"sp":{"value":6174682320},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623068,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":144,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":3957028,"symbol":"G1ConcurrentMarkThread::run_service()","symbolLocation":156,"imageIndex":2},{"imageOffset":2876088,"symbol":"ConcurrentGCThread::run()","symbolLocation":36,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501624,"name":"Java: G1 Conc#0","threadState":{"x":[{"value":14},{"value":1},{"value":0},{"value":4811341760},{"value":4811325568},{"value":1},{"value":18446744072631617535},{"value":18446726482597246976},{"value":6176829112},{"value":4309160480},{"value":4294967295},{"value":2199023256066},{"value":512},{"value":2199023256064},{"value":11},{"value":2184519612256},{"value":18446744073709551580},{"value":4309163040},{"value":0},{"value":49840993784},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":1},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840993768},{"value":49840993780},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":1610616832},"fp":{"value":6176829088},"sp":{"value":6176829072},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501625,"name":"Java: G1 Refine#0","threadState":{"x":[{"value":316},{"value":0},{"value":25856},{"value":0},{"value":0},{"value":160},{"value":0},{"value":61000000},{"value":6178974840},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845637264},{"value":49841797120},{"value":6178975968},{"value":61000000},{"value":0},{"value":25856},{"value":32513},{"value":32768},{"value":4412483248,"symbolLocation":0,"symbol":"SuspendibleThreadSet::_suspend_all"},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6178974960},"sp":{"value":6178974816},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":3993596,"symbol":"G1PrimaryConcurrentRefineThread::wait_for_completed_buffers()","symbolLocation":72,"imageIndex":2},{"imageOffset":3992324,"symbol":"G1ConcurrentRefineThread::run_service()","symbolLocation":136,"imageIndex":2},{"imageOffset":2876088,"symbol":"ConcurrentGCThread::run()","symbolLocation":36,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501626,"name":"Java: G1 Service","threadState":{"x":[{"value":260},{"value":0},{"value":6656},{"value":0},{"value":0},{"value":160},{"value":0},{"value":999999000},{"value":6181121368},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845637504},{"value":49841797440},{"value":6181122272},{"value":999999000},{"value":0},{"value":6656},{"value":7425},{"value":7680},{"value":14067037542},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6181121488},"sp":{"value":6181121344},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":4277004,"symbol":"G1ServiceThread::wait_for_task()","symbolLocation":172,"imageIndex":2},{"imageOffset":4277536,"symbol":"G1ServiceThread::run_service()","symbolLocation":40,"imageIndex":2},{"imageOffset":2876088,"symbol":"ConcurrentGCThread::run()","symbolLocation":36,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501627,"name":"Java: VM Periodic Task Thread","threadState":{"x":[{"value":316},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":0},{"value":50000000},{"value":6183267656},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845488368},{"value":49840982208},{"value":6183268576},{"value":50000000},{"value":0},{"value":768},{"value":64769},{"value":65024},{"value":4835703278458516699},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6183267776},"sp":{"value":6183267632},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":8441656,"symbol":"WatcherThread::sleep() const","symbolLocation":152,"imageIndex":2},{"imageOffset":8441876,"symbol":"WatcherThread::run()","symbolLocation":60,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501629,"name":"Java: VM Thread","threadState":{"x":[{"value":260},{"value":0},{"value":37632},{"value":0},{"value":0},{"value":160},{"value":1},{"value":0},{"value":6185413944},{"value":0},{"value":512},{"value":2199023256066},{"value":2199023256066},{"value":512},{"value":0},{"value":2199023256064},{"value":305},{"value":8324043672},{"value":0},{"value":49845491168},{"value":49840984448},{"value":6185414880},{"value":0},{"value":1},{"value":37632},{"value":37633},{"value":37888},{"value":4412233896,"symbolLocation":0,"symbol":"GuaranteedSafepointInterval"},{"value":4412445370,"symbolLocation":0,"symbol":"HandshakeALot"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6185414064},"sp":{"value":6185413920},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":11067152,"symbol":"VMThread::wait_for_operation()","symbolLocation":444,"imageIndex":2},{"imageOffset":11063548,"symbol":"VMThread::run()","symbolLocation":188,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501630,"frames":[],"threadState":{"x":[{"value":6185988096},{"value":31495},{"value":6185451520},{"value":0},{"value":409604},{"value":18446744073709551615},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":0},"cpsr":{"value":4096},"fp":{"value":0},"sp":{"value":6185988096},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478760852},"far":{"value":0}}},{"id":501633,"name":"Java: Reference Handler","threadState":{"x":[{"value":260},{"value":0},{"value":3072},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6188705768},{"value":0},{"value":768},{"value":3298534884098},{"value":3298534884098},{"value":768},{"value":0},{"value":3298534884096},{"value":305},{"value":8324043672},{"value":0},{"value":49845490848},{"value":49840984192},{"value":6188708064},{"value":0},{"value":0},{"value":3072},{"value":3073},{"value":3328},{"value":0},{"value":4309217552}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6188705888},"sp":{"value":6188705744},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623068,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":144,"imageIndex":2},{"imageOffset":8351440,"symbol":"Monitor::wait(unsigned long long)","symbolLocation":124,"imageIndex":2},{"imageOffset":5826952,"symbol":"JVM_WaitForReferencePendingList","symbolLocation":200,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501634,"name":"Java: Finalizer","threadState":{"x":[{"value":260},{"value":0},{"value":1792},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6190851272},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845668648},{"value":49845668712},{"value":6190854368},{"value":0},{"value":0},{"value":1792},{"value":1793},{"value":2048},{"value":49845644464},{"value":2}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6190851392},"sp":{"value":6190851248},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8619692,"symbol":"PlatformEvent::park()","symbolLocation":120,"imageIndex":2},{"imageOffset":8479060,"symbol":"ObjectMonitor::wait(long, bool, JavaThread*)","symbolLocation":1340,"imageIndex":2},{"imageOffset":10307172,"symbol":"ObjectSynchronizer::wait(Handle, long, JavaThread*)","symbolLocation":292,"imageIndex":2},{"imageOffset":5737308,"symbol":"JVM_MonitorWait","symbolLocation":440,"imageIndex":2},{"imageOffset":4689438812,"imageIndex":31},{"imageOffset":4566855872,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501635,"name":"Java: Signal Dispatcher","threadState":{"x":[{"value":14},{"value":35},{"value":0},{"value":6193000552},{"value":6192999960},{"value":23},{"value":32199646364590448},{"value":18446726482597246976},{"value":10},{"value":4412873760,"symbolLocation":128,"symbol":"_MergedGlobals"},{"value":4294967295},{"value":17},{"value":17},{"value":49845657616},{"value":4306668536},{"value":49837047808},{"value":18446744073709551580},{"value":49841984352},{"value":0},{"value":4309093824},{"value":49842063104},{"value":4309093824},{"value":4412873632,"symbolLocation":0,"symbol":"_MergedGlobals"},{"value":49845645424},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":2},{"value":4},{"value":6},{"value":49842064212}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":1610616832},"fp":{"value":6192999440},"sp":{"value":6192999424},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":9775748,"symbol":"os::signal_wait()","symbolLocation":180,"imageIndex":2},{"imageOffset":8568988,"symbol":"signal_thread_entry(JavaThread*, JavaThread*)","symbolLocation":76,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501636,"name":"Java: Service Thread","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6195145864},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845485888},{"value":49840980224},{"value":6195146976},{"value":0},{"value":0},{"value":1024},{"value":1025},{"value":1280},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6195145984},"sp":{"value":6195145840},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623068,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":144,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":9209576,"symbol":"ServiceThread::service_thread_entry(JavaThread*, JavaThread*)","symbolLocation":520,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501637,"name":"Java: Monitor Deflation Thread","threadState":{"x":[{"value":316},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":0},{"value":250000000},{"value":6197292312},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845485808},{"value":49840980160},{"value":6197293280},{"value":250000000},{"value":0},{"value":0},{"value":13057},{"value":13312},{"value":4412479200,"symbolLocation":0,"symbol":"SafepointSynchronize::_state"},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6197292432},"sp":{"value":6197292288},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":8304592,"symbol":"MonitorDeflationThread::monitor_deflation_thread_entry(JavaThread*, JavaThread*)","symbolLocation":252,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501638,"name":"Java: C2 CompilerThread0","threadState":{"x":[{"value":49763978432},{"value":49763978432},{"value":0},{"value":49763978504},{"value":6199425696},{"value":10},{"value":11},{"value":8},{"value":4401053328,"symbolLocation":0,"symbol":"ConLNode::Opcode() const"},{"value":1},{"value":49763978432},{"value":49807325384},{"value":45},{"value":49807325744},{"value":49763978432},{"value":49842168544},{"value":72},{"value":49841984832},{"value":0},{"value":49763978432},{"value":6199434432},{"value":49793386880},{"value":2026},{"value":1999},{"value":49785307920},{"value":2048},{"value":49794544920},{"value":0},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4406940504},"cpsr":{"value":1610616832},"fp":{"value":6199425648},"sp":{"value":6199425584},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":4401053328},"far":{"value":0}},"frames":[{"imageOffset":2539152,"symbol":"ConLNode::Opcode() const","symbolLocation":0,"imageIndex":2},{"imageOffset":8426328,"symbol":"Node::destruct(PhaseValues*)","symbolLocation":468,"imageIndex":2},{"imageOffset":8816792,"symbol":"PhaseValues::uncached_makecon(Type const*)","symbolLocation":76,"imageIndex":2},{"imageOffset":8816312,"symbol":"PhaseValues::makecon(Type const*)","symbolLocation":288,"imageIndex":2},{"imageOffset":8825900,"symbol":"PhaseIterGVN::transform_old(Node*)","symbolLocation":864,"imageIndex":2},{"imageOffset":8823980,"symbol":"PhaseIterGVN::optimize()","symbolLocation":180,"imageIndex":2},{"imageOffset":7661084,"symbol":"PhaseMacroExpand::expand_macro_nodes()","symbolLocation":1736,"imageIndex":2},{"imageOffset":2726288,"symbol":"Compile::Optimize()","symbolLocation":2872,"imageIndex":2},{"imageOffset":2719996,"symbol":"Compile::Compile(ciEnv*, ciMethod*, int, Options, DirectiveSet*)","symbolLocation":3220,"imageIndex":2},{"imageOffset":2028096,"symbol":"C2Compiler::compile_method(ciEnv*, ciMethod*, int, bool, DirectiveSet*)","symbolLocation":348,"imageIndex":2},{"imageOffset":2788672,"symbol":"CompileBroker::invoke_compiler_on_method(CompileTask*)","symbolLocation":1212,"imageIndex":2},{"imageOffset":2787016,"symbol":"CompileBroker::compiler_thread_loop()","symbolLocation":1048,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501639,"name":"Java: C1 CompilerThread0","threadState":{"x":[{"value":260},{"value":0},{"value":1104896},{"value":0},{"value":0},{"value":160},{"value":4},{"value":999999000},{"value":6201584440},{"value":0},{"value":347648},{"value":1493136790867458},{"value":1493136790867458},{"value":347648},{"value":0},{"value":1493136790867456},{"value":305},{"value":8324043672},{"value":0},{"value":49845487488},{"value":49840981504},{"value":6201585888},{"value":999999000},{"value":4},{"value":1104896},{"value":1104897},{"value":1105152},{"value":49842010944},{"value":4412219392,"symbolLocation":8,"symbol":"LogTagSetMapping<(LogTag::type)47, (LogTag::type)55, (LogTag::type)0, (LogTag::type)0, (LogTag::type)0, (LogTag::type)0>::_tagset"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6201584560},"sp":{"value":6201584416},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623196,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":272,"imageIndex":2},{"imageOffset":8351440,"symbol":"Monitor::wait(unsigned long long)","symbolLocation":124,"imageIndex":2},{"imageOffset":2773092,"symbol":"CompileQueue::get(CompilerThread*)","symbolLocation":748,"imageIndex":2},{"imageOffset":2786720,"symbol":"CompileBroker::compiler_thread_loop()","symbolLocation":752,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501640,"name":"Java: Notification Thread","threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6203731208},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49845485968},{"value":49840980288},{"value":6203732192},{"value":0},{"value":0},{"value":0},{"value":1},{"value":256},{"value":0},{"value":4412444080,"symbolLocation":0,"symbol":"DCmdFactory::_has_pending_jmx_notification"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6203731328},"sp":{"value":6203731184},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8623068,"symbol":"PlatformMonitor::wait(unsigned long long)","symbolLocation":144,"imageIndex":2},{"imageOffset":8351292,"symbol":"Monitor::wait_without_safepoint_check(unsigned long long)","symbolLocation":48,"imageIndex":2},{"imageOffset":8443372,"symbol":"NotificationThread::notification_thread_entry(JavaThread*, JavaThread*)","symbolLocation":164,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501641,"name":"Java: Common-Cleaner","threadState":{"x":[{"value":260},{"value":0},{"value":5120},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998333},{"value":6205875032},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49842071776},{"value":49842071840},{"value":6205878496},{"value":999998333},{"value":59},{"value":5120},{"value":5121},{"value":5376},{"value":0},{"value":49842070272}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6205875152},"sp":{"value":6205875008},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4574993932,"imageIndex":31},{"imageOffset":4681258236,"imageIndex":31},{"imageOffset":4562612512,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501673,"frames":[],"threadState":{"x":[{"value":6206451712},{"value":44547},{"value":6205915136},{"value":0},{"value":409604},{"value":18446744073709551615},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":0},"cpsr":{"value":4096},"fp":{"value":0},"sp":{"value":6206451712},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478760852},"far":{"value":0}}},{"id":501678,"frames":[],"threadState":{"x":[{"value":6207025152},{"value":63751},{"value":6206488576},{"value":0},{"value":409604},{"value":18446744073709551615},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":0},"cpsr":{"value":4096},"fp":{"value":0},"sp":{"value":6207025152},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478760852},"far":{"value":0}}},{"id":501684,"name":"com.apple.NSEventThread","threadState":{"x":[{"value":268451845},{"value":21592279046},{"value":8589934592,"objc-selector":""},{"value":270492745334784},{"value":0},{"value":270492745334784},{"value":2},{"value":4294967295},{"value":0},{"value":17179869184},{"value":0},{"value":2},{"value":0},{"value":0},{"value":62979},{"value":0},{"value":18446744073709551569},{"value":8324045552},{"value":0},{"value":4294967295},{"value":2},{"value":270492745334784},{"value":0},{"value":270492745334784},{"value":6207594632},{"value":8589934592,"objc-selector":""},{"value":21592279046},{"value":18446744073709550527},{"value":4412409862,"symbolLocation":82,"symbol":"CRSCommandListenerThread::_buffer"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478581800},"cpsr":{"value":4096},"fp":{"value":6207594480},"sp":{"value":6207594400},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478507060},"far":{"value":0}},"frames":[{"imageOffset":3124,"symbol":"mach_msg2_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":77864,"symbol":"mach_msg2_internal","symbolLocation":76,"imageIndex":25},{"imageOffset":39308,"symbol":"mach_msg_overwrite","symbolLocation":484,"imageIndex":25},{"imageOffset":4020,"symbol":"mach_msg","symbolLocation":24,"imageIndex":25},{"imageOffset":392080,"symbol":"__CFRunLoopServiceMachPort","symbolLocation":160,"imageIndex":26},{"imageOffset":386280,"symbol":"__CFRunLoopRun","symbolLocation":1188,"imageIndex":26},{"imageOffset":1147740,"symbol":"_CFRunLoopRunSpecificWithOptions","symbolLocation":532,"imageIndex":26},{"imageOffset":720052,"symbol":"_NSEventThread","symbolLocation":184,"imageIndex":28},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501686,"name":"Java: AWT-Shutdown","threadState":{"x":[{"value":260},{"value":0},{"value":33280},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6209742088},{"value":0},{"value":256},{"value":1099511628034},{"value":1099511628034},{"value":256},{"value":0},{"value":1099511628032},{"value":305},{"value":8324043672},{"value":0},{"value":49850772776},{"value":49850772840},{"value":6209745120},{"value":0},{"value":0},{"value":33280},{"value":33281},{"value":33536},{"value":49851153248},{"value":2}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6209742208},"sp":{"value":6209742064},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8619692,"symbol":"PlatformEvent::park()","symbolLocation":120,"imageIndex":2},{"imageOffset":8479060,"symbol":"ObjectMonitor::wait(long, bool, JavaThread*)","symbolLocation":1340,"imageIndex":2},{"imageOffset":10307172,"symbol":"ObjectSynchronizer::wait(Handle, long, JavaThread*)","symbolLocation":292,"imageIndex":2},{"imageOffset":5737308,"symbol":"JVM_MonitorWait","symbolLocation":440,"imageIndex":2},{"imageOffset":4689438812,"imageIndex":31},{"imageOffset":4566855872,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501727,"name":"Java: DefaultDispatcher-worker-1","threadState":{"x":[{"value":260},{"value":0},{"value":4352},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6211888600},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49811093216},{"value":49811093280},{"value":6211891424},{"value":0},{"value":60},{"value":4352},{"value":4353},{"value":4608},{"value":0},{"value":49811091712}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6211888720},"sp":{"value":6211888576},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4567738896,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501728,"name":"Java: DefaultDispatcher-worker-2","threadState":{"x":[{"value":260},{"value":0},{"value":10496},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6214035016},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49811095008},{"value":49811095072},{"value":6214037728},{"value":0},{"value":60},{"value":10496},{"value":10497},{"value":10752},{"value":0},{"value":49811093504}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6214035136},"sp":{"value":6214034992},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571996880,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501729,"name":"Java: DefaultDispatcher-worker-3","threadState":{"x":[{"value":260},{"value":0},{"value":8448},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999000},{"value":6216181208},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49794975200},{"value":49794975264},{"value":6216184032},{"value":999999000},{"value":59},{"value":8448},{"value":8449},{"value":8704},{"value":0},{"value":49794973696}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6216181328},"sp":{"value":6216181184},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4567738896,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501730,"name":"Java: kotlinx.coroutines.DefaultExecutor","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":21},{"value":592947333},{"value":6218327608},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49794978784},{"value":49794978848},{"value":6218330336},{"value":592947333},{"value":21},{"value":768},{"value":1025},{"value":1280},{"value":4348149760},{"value":49794977280}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6218327728},"sp":{"value":6218327584},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501731,"name":"Java: Reference Cleaner","threadState":{"x":[{"value":260},{"value":0},{"value":1792},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998666},{"value":6220473320},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49794980576},{"value":49794980640},{"value":6220476640},{"value":999998666},{"value":59},{"value":1792},{"value":1793},{"value":2048},{"value":0},{"value":49794979072}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6220473440},"sp":{"value":6220473296},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4574993932,"imageIndex":31},{"imageOffset":4681258236,"imageIndex":31},{"imageOffset":4562612512,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"triggered":true,"id":501739,"name":"Java: AWT-EventQueue-0","threadState":{"x":[{"value":59},{"value":7190150079,"symbolLocation":191,"symbol":".str.60"},{"value":7190150252,"symbolLocation":364,"symbol":".str.60"},{"value":38},{"value":0},{"value":0},{"value":97},{"value":18},{"value":6222622720},{"value":8300994688,"symbolLocation":0,"symbol":"_main_thread"},{"value":49576352195},{"value":3},{"value":3},{"value":49805623968},{"value":8301191952,"symbolLocation":0,"symbol":"OBJC_METACLASS_$_NSThread"},{"value":8301191952,"symbolLocation":0,"symbol":"OBJC_METACLASS_$_NSThread"},{"value":6478763748,"symbolLocation":0,"symbol":"pthread_main_np"},{"value":8324053232},{"value":0},{"value":8304215472,"symbolLocation":0,"symbol":"WebKit::flag"},{"value":7176196740,"symbolLocation":0,"symbol":"void std::__1::__call_once_proxy[abi:sn200100]<std::__1::tuple<WebKit::InitializeWebKit2()::$_0&&>>(void*)"},{"value":6222617248},{"value":0},{"value":20775335448},{"value":6222618216},{"value":20861789008},{"value":6222617488},{"value":20836043080},{"value":49794982656}],"flavor":"ARM_THREAD_STATE64","lr":{"value":7176197068},"cpsr":{"value":2147487744},"fp":{"value":6222617136},"sp":{"value":6222617136},"esr":{"value":4060136561,"description":"(Breakpoint) pointer authentication trap IB"},"pc":{"value":7176197132,"matchesCrashFrame":1},"far":{"value":0}},"frames":[{"imageOffset":4625420,"symbol":"WebKit::runInitializationCode(void*)","symbolLocation":88,"imageIndex":34},{"imageOffset":50404,"symbol":"std::__1::__call_once(unsigned long volatile&, void*, void (*)(void*))","symbolLocation":196,"imageIndex":35},{"imageOffset":119148,"symbol":"WebKit::InitializeWebKit2()","symbolLocation":88,"imageIndex":34},{"imageOffset":9246740,"symbol":"API::Object::Object()","symbolLocation":124,"imageIndex":34},{"imageOffset":5747172,"symbol":"-[WKWebViewConfiguration init]","symbolLocation":108,"imageIndex":34},{"imageOffset":127968,"sourceLine":44,"sourceFile":"WebViewHelper.kt","symbol":"kfun:com.prof18.webview#createWebView(kotlinx.cinterop.CPointer<kotlinx.cinterop.CPointerVarOf<kotlinx.cinterop.CPointer<com.prof18.jni.JNINativeInterface_>>>;kotlinx.cinterop.CPointer<cnames.structs._jobject>){}kotlin.Int","imageIndex":19,"symbolLocation":268},{"imageOffset":244096,"symbol":"Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_createWebView","symbolLocation":80,"imageIndex":19},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4690667992,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501741,"name":"Java: GC Thread#1","threadState":{"x":[{"value":14},{"value":32},{"value":5},{"value":68719460488},{"value":49720211040},{"value":0},{"value":18446744072631617535},{"value":18446726482597246976},{"value":6224768696},{"value":49858707456},{"value":4294967295},{"value":72},{"value":4302313600},{"value":49756986880},{"value":49756986880},{"value":49720197120},{"value":18446744073709551580},{"value":49806725344},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":5},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6224768672},"sp":{"value":6224768656},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501742,"name":"Java: GC Thread#2","threadState":{"x":[{"value":14},{"value":32},{"value":6},{"value":68719460488},{"value":49720210976},{"value":0},{"value":18446744072631617535},{"value":18446726482597246976},{"value":6226915000},{"value":49858708480},{"value":4294967295},{"value":72},{"value":4302683968},{"value":49732144128},{"value":49732144128},{"value":49720197120},{"value":18446744073709551580},{"value":49806725504},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":6},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6226914976},"sp":{"value":6226914960},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501743,"name":"Java: GC Thread#3","threadState":{"x":[{"value":14},{"value":32},{"value":1},{"value":68719460488},{"value":6229058271},{"value":6229060544},{"value":0},{"value":996000},{"value":6229061304},{"value":49858709504},{"value":4294967295},{"value":72},{"value":4302299584},{"value":0},{"value":446464},{"value":5},{"value":18446744073709551580},{"value":49806725664},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":1},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6229061280},"sp":{"value":6229061264},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501744,"name":"Java: GC Thread#4","threadState":{"x":[{"value":14},{"value":32},{"value":4},{"value":68719460488},{"value":32},{"value":7},{"value":6597069769728},{"value":1000000},{"value":6231207608},{"value":49858710528},{"value":4294967295},{"value":72},{"value":4294967293},{"value":14293651161088},{"value":3328},{"value":256},{"value":18446744073709551580},{"value":49806725824},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":4},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6231207584},"sp":{"value":6231207568},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501745,"name":"Java: GC Thread#5","threadState":{"x":[{"value":14},{"value":32},{"value":0},{"value":68719460488},{"value":6233353224},{"value":6233353152},{"value":0},{"value":1000000},{"value":6233353912},{"value":49858711552},{"value":4294967295},{"value":72},{"value":4302300160},{"value":0},{"value":20480},{"value":5},{"value":18446744073709551580},{"value":49806725984},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":0},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6233353888},"sp":{"value":6233353872},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501761,"name":"Java: Java2D Queue Flusher","threadState":{"x":[{"value":316},{"value":0},{"value":40192},{"value":0},{"value":0},{"value":160},{"value":0},{"value":100000000},{"value":6235497848},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49853098280},{"value":49853098344},{"value":6235500768},{"value":100000000},{"value":0},{"value":40192},{"value":40449},{"value":40704},{"value":49858737600},{"value":2}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6235497968},"sp":{"value":6235497824},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8620240,"symbol":"PlatformEvent::park_nanos(long)","symbolLocation":332,"imageIndex":2},{"imageOffset":8479028,"symbol":"ObjectMonitor::wait(long, bool, JavaThread*)","symbolLocation":1308,"imageIndex":2},{"imageOffset":10307172,"symbol":"ObjectSynchronizer::wait(Handle, long, JavaThread*)","symbolLocation":292,"imageIndex":2},{"imageOffset":5737308,"symbol":"JVM_MonitorWait","symbolLocation":440,"imageIndex":2},{"imageOffset":4689438812,"imageIndex":31},{"imageOffset":4566855872,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501768,"name":"Java: Java2D Disposer","threadState":{"x":[{"value":260},{"value":0},{"value":1536},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6237643624},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49795508448},{"value":49795508512},{"value":6237647072},{"value":0},{"value":0},{"value":1536},{"value":1537},{"value":1792},{"value":0},{"value":49795506944}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6237643744},"sp":{"value":6237643600},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621280,"symbol":"Parker::park(bool, long)","symbolLocation":512,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4576090600,"imageIndex":31},{"imageOffset":4576373684,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501807,"name":"Java: TimerQueue","threadState":{"x":[{"value":260},{"value":0},{"value":14080},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6239790216},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49791017440},{"value":49791017504},{"value":6239793376},{"value":0},{"value":0},{"value":14080},{"value":14337},{"value":14592},{"value":4348149760},{"value":49791015936}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6239790336},"sp":{"value":6239790192},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621280,"symbol":"Parker::park(bool, long)","symbolLocation":512,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4576090600,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501809,"name":"Java: GC Thread#6","threadState":{"x":[{"value":14},{"value":1},{"value":7},{"value":68719460488},{"value":6241938271},{"value":0},{"value":18446744072631617535},{"value":18446726482597246976},{"value":6241939128},{"value":49860174848},{"value":4294967295},{"value":72},{"value":4302300736},{"value":13933011355720704},{"value":155648},{"value":11},{"value":18446744073709551580},{"value":49795900000},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":7},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":1610616832},"fp":{"value":6241939104},"sp":{"value":6241939088},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501820,"name":"Java: GC Thread#7","threadState":{"x":[{"value":14},{"value":32},{"value":3},{"value":68719460488},{"value":32},{"value":6},{"value":0},{"value":1000000},{"value":6245232312},{"value":49861797888},{"value":4294967295},{"value":72},{"value":64768},{"value":3072},{"value":768},{"value":0},{"value":18446744073709551580},{"value":49782743808},{"value":0},{"value":49840992888},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":3},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840992872},{"value":49840992884},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6245232288},"sp":{"value":6245232272},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501821,"name":"Java: G1 Conc#1","threadState":{"x":[{"value":14},{"value":49841983552},{"value":0},{"value":4813897664},{"value":4813881472},{"value":1},{"value":18446744072103133183},{"value":1},{"value":6247378616},{"value":49861798912},{"value":4294967295},{"value":2199023256066},{"value":512},{"value":2199023256064},{"value":13},{"value":49791156224},{"value":18446744073709551580},{"value":49782743648},{"value":0},{"value":49840993784},{"value":4412384080,"symbolLocation":0,"symbol":"WorkerThread::_worker_id"},{"value":0},{"value":1},{"value":6474593016,"symbolLocation":0,"symbol":"_tlv_get_addr"},{"value":4294967295},{"value":49840993768},{"value":49840993780},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4407715236},"cpsr":{"value":536875008},"fp":{"value":6247378592},"sp":{"value":6247378576},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478506928},"far":{"value":0}},"frames":[{"imageOffset":2992,"symbol":"semaphore_wait_trap","symbolLocation":8,"imageIndex":25},{"imageOffset":9201060,"symbol":"OSXSemaphore::wait()","symbolLocation":24,"imageIndex":2},{"imageOffset":11245616,"symbol":"WorkerThread::run()","symbolLocation":84,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501828,"frames":[],"threadState":{"x":[{"value":6242512896},{"value":118531},{"value":6241976320},{"value":0},{"value":409604},{"value":18446744073709551615},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":0},"cpsr":{"value":4096},"fp":{"value":0},"sp":{"value":6242512896},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478760852},"far":{"value":0}}},{"recursionInfoArray":[{"hottestElided":36,"coldestElided":38,"depth":9,"keyFrame":{"imageOffset":4681257340,"imageIndex":31}}],"id":501838,"originalLength":54,"name":"Java: pool-1-thread-1","threadState":{"x":[{"value":49799512064},{"value":49799512064},{"value":18446726483666796544},{"value":1},{"value":49720235392},{"value":49720614496},{"value":49720235944},{"value":6250076236},{"value":6639351592,"symbolLocation":0,"symbol":"vImageCGConverter_Destroy"},{"value":49799512072},{"value":25998812803968},{"value":8301132264,"symbolLocation":0,"symbol":"__CFRuntimeClassTables"},{"value":3008628890038608},{"value":2990671631772672},{"value":28672},{"value":108},{"value":11191275524},{"value":8301126840,"symbolLocation":0,"symbol":"OBJC_CLASS_$___NSCFType"},{"value":0},{"value":49799512064},{"value":115},{"value":8393556888,"symbolLocation":0,"symbol":"CGConverterClassRegister.CGConverterClass"},{"value":18446744073709551615},{"value":25998812803968},{"value":8266498048,"symbolLocation":896,"symbol":"__last_exception_os_log_pack__"},{"value":18446726481523507200},{"value":23},{"value":0},{"value":6250072912}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6480195704},"cpsr":{"value":1610616832},"fp":{"value":6250071280},"sp":{"value":6250071264},"esr":{"value":2181038091,"description":"(Instruction Abort) Access flag fault"},"pc":{"value":7328776432},"far":{"value":0}},"frames":[{"imageOffset":12528,"symbol":"vImageConverter_Finalize","symbolLocation":36,"imageIndex":36},{"imageOffset":1155192,"symbol":"_CFRelease","symbolLocation":296,"imageIndex":26},{"imageOffset":278524,"symbol":"provider_for_destination_release_info","symbolLocation":76,"imageIndex":37},{"imageOffset":278208,"symbol":"data_provider_finalize","symbolLocation":64,"imageIndex":37},{"imageOffset":49280,"symbol":"data_provider_retain_count","symbolLocation":96,"imageIndex":37},{"imageOffset":1156228,"symbol":"_CFRelease","symbolLocation":1332,"imageIndex":26},{"imageOffset":262764,"symbol":"img_data_lock","symbolLocation":8044,"imageIndex":37},{"imageOffset":235288,"symbol":"CGSImageDataLock","symbolLocation":1168,"imageIndex":37},{"imageOffset":233100,"symbol":"ripc_AcquireRIPImageData","symbolLocation":1420,"imageIndex":37},{"imageOffset":226536,"symbol":"ripc_DrawImage","symbolLocation":808,"imageIndex":37},{"imageOffset":225320,"symbol":"CGContextDrawImageWithOptions","symbolLocation":1032,"imageIndex":37},{"imageOffset":224096,"symbol":"CGContextDrawImage","symbolLocation":556,"imageIndex":37},{"imageOffset":655060,"symbol":"(anonymous namespace)::TCGImageData::DrawAtPoint(CGContext*, CGPoint) const","symbolLocation":240,"imageIndex":38},{"imageOffset":680512,"symbol":"DrawGlyphsAtPositions(TFont const*, unsigned short const*, CGPoint const*, unsigned long, CGContext*, (anonymous namespace)::DrawGlyphsConfiguration const&)::$_1::operator()(CGAffineTransform, CGAffineTransform) const","symbolLocation":292,"imageIndex":38},{"imageOffset":658008,"symbol":"DrawGlyphsAtPositions(TFont const*, unsigned short const*, CGPoint const*, unsigned long, CGContext*, (anonymous namespace)::DrawGlyphsConfiguration const&)","symbolLocation":2496,"imageIndex":38},{"imageOffset":282336,"symbol":"CTFontDrawGlyphs","symbolLocation":248,"imageIndex":38},{"imageOffset":2170016,"symbol":"SkScalerContext_Mac::Offscreen::getCG(SkScalerContext_Mac const&, SkGlyph const&, unsigned short, unsigned long*, bool)","symbolLocation":1624,"imageIndex":8},{"imageOffset":2170648,"symbol":"SkScalerContext_Mac::generateImage(SkGlyph const&, void*)","symbolLocation":84,"imageIndex":8},{"imageOffset":1110436,"symbol":"SkScalerContext::getImage(SkGlyph const&)","symbolLocation":576,"imageIndex":8},{"imageOffset":624536,"symbol":"SkGlyph::setImage(SkArenaAlloc*, SkScalerContext*)","symbolLocation":92,"imageIndex":8},{"imageOffset":1172352,"symbol":"SkStrike::prepareImages(SkSpan<SkPackedGlyphID const>, SkGlyph const**)","symbolLocation":148,"imageIndex":8},{"imageOffset":1183028,"symbol":"SkBulkGlyphMetricsAndImages::glyph(SkPackedGlyphID)","symbolLocation":88,"imageIndex":8},{"imageOffset":3168532,"symbol":"sktext::gpu::GlyphVector::regenerateAtlasForGanesh(int, int, skgpu::MaskFormat, int, GrMeshDrawTarget*)","symbolLocation":440,"imageIndex":8},{"imageOffset":3599444,"symbol":"non-virtual thunk to (anonymous namespace)::DirectMaskSubRun::regenerateAtlas(int, int, std::__1::function<std::__1::tuple<bool, int> (sktext::gpu::GlyphVector*, int, int, skgpu::MaskFormat, int)>) const","symbolLocation":76,"imageIndex":8},{"imageOffset":2871792,"symbol":"skgpu::ganesh::AtlasTextOp::onPrepareDraws(GrMeshDrawTarget*)","symbolLocation":1428,"imageIndex":8},{"imageOffset":3006060,"symbol":"skgpu::ganesh::OpsTask::onPrepare(GrOpFlushState*)","symbolLocation":460,"imageIndex":8},{"imageOffset":2449016,"symbol":"GrRenderTask::prepare(GrOpFlushState*)","symbolLocation":104,"imageIndex":8},{"imageOffset":2339508,"symbol":"GrDrawingManager::executeRenderTasks(GrOpFlushState*)","symbolLocation":92,"imageIndex":8},{"imageOffset":2338076,"symbol":"GrDrawingManager::flush(SkSpan<GrSurfaceProxy*>, SkSurfaces::BackendSurfaceAccess, GrFlushInfo const&, skgpu::MutableTextureState const*)","symbolLocation":1284,"imageIndex":8},{"imageOffset":2340880,"symbol":"GrDrawingManager::flushSurfaces(SkSpan<GrSurfaceProxy*>, SkSurfaces::BackendSurfaceAccess, GrFlushInfo const&, skgpu::MutableTextureState const*)","symbolLocation":168,"imageIndex":8},{"imageOffset":151456,"symbol":"Java_org_jetbrains_skia_DirectContextKt_DirectContext_1nFlushDefault","symbolLocation":36,"imageIndex":8},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4690667992,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501840,"name":"Java: Timer-0","threadState":{"x":[{"value":260},{"value":0},{"value":256},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6252242264},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49866971432},{"value":49866971496},{"value":6252245216},{"value":0},{"value":60},{"value":256},{"value":257},{"value":512},{"value":49866912176},{"value":2}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6252242384},"sp":{"value":6252242240},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8620240,"symbol":"PlatformEvent::park_nanos(long)","symbolLocation":332,"imageIndex":2},{"imageOffset":8479028,"symbol":"ObjectMonitor::wait(long, bool, JavaThread*)","symbolLocation":1308,"imageIndex":2},{"imageOffset":10307172,"symbol":"ObjectSynchronizer::wait(Handle, long, JavaThread*)","symbolLocation":292,"imageIndex":2},{"imageOffset":5737308,"symbol":"JVM_MonitorWait","symbolLocation":440,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501848,"name":"CVDisplayLink","threadState":{"x":[{"value":316},{"value":0},{"value":0},{"value":0},{"value":0},{"value":65704},{"value":0},{"value":7399250},{"value":309249},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49848303672},{"value":49848303736},{"value":1},{"value":7399250},{"value":0},{"value":0},{"value":309249},{"value":309504},{"value":333909471850},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782728},"cpsr":{"value":2684358656},"fp":{"value":6252817840},"sp":{"value":6252817696},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28936,"symbol":"_pthread_cond_wait","symbolLocation":1028,"imageIndex":32},{"imageOffset":15164,"symbol":"CVDisplayLink::waitUntil(unsigned long long)","symbolLocation":336,"imageIndex":40},{"imageOffset":11300,"symbol":"CVDisplayLink::runIOThread()","symbolLocation":500,"imageIndex":40},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501861,"name":"Java: DefaultDispatcher-worker-4","threadState":{"x":[{"value":260},{"value":0},{"value":4864},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999000},{"value":6254962216},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777289440},{"value":49777289504},{"value":6254964960},{"value":999999000},{"value":59},{"value":4864},{"value":4865},{"value":5120},{"value":0},{"value":49777287936}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6254962336},"sp":{"value":6254962192},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501862,"name":"Java: DefaultDispatcher-worker-6","threadState":{"x":[{"value":260},{"value":0},{"value":7168},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6257108520},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777291232},{"value":49777291296},{"value":6257111264},{"value":0},{"value":60},{"value":7168},{"value":7169},{"value":7424},{"value":0},{"value":49777289728}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6257108640},"sp":{"value":6257108496},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501863,"name":"Java: DefaultDispatcher-worker-5","threadState":{"x":[{"value":260},{"value":0},{"value":5632},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999000},{"value":6259254824},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777293024},{"value":49777293088},{"value":6259257568},{"value":999999000},{"value":59},{"value":5632},{"value":5633},{"value":5888},{"value":0},{"value":49777291520}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6259254944},"sp":{"value":6259254800},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501864,"name":"Java: DefaultDispatcher-worker-7","threadState":{"x":[{"value":260},{"value":0},{"value":17152},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6261401160},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777294816},{"value":49777294880},{"value":6261403872},{"value":0},{"value":60},{"value":17152},{"value":17409},{"value":17664},{"value":4348149760},{"value":49777293312}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6261401280},"sp":{"value":6261401136},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571996880,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501865,"name":"Java: DefaultDispatcher-worker-8","threadState":{"x":[{"value":260},{"value":0},{"value":1280},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6263547432},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762600416},{"value":49762600480},{"value":6263550176},{"value":0},{"value":60},{"value":1280},{"value":1281},{"value":1536},{"value":0},{"value":49762598912}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6263547552},"sp":{"value":6263547408},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501866,"name":"Java: DefaultDispatcher-worker-11","threadState":{"x":[{"value":260},{"value":0},{"value":20480},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6265693768},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762602208},{"value":49762602272},{"value":6265696480},{"value":0},{"value":60},{"value":20480},{"value":20737},{"value":20992},{"value":4348149760},{"value":49762600704}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6265693888},"sp":{"value":6265693744},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571996880,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501867,"name":"Java: DefaultDispatcher-worker-10","threadState":{"x":[{"value":260},{"value":0},{"value":1280},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6267840040},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762604000},{"value":49762604064},{"value":6267842784},{"value":0},{"value":60},{"value":1280},{"value":1281},{"value":1536},{"value":0},{"value":49762602496}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6267840160},"sp":{"value":6267840016},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501868,"name":"Java: DefaultDispatcher-worker-9","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6269986344},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762605792},{"value":49762605856},{"value":6269989088},{"value":0},{"value":60},{"value":1024},{"value":1025},{"value":1280},{"value":0},{"value":49762604288}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6269986464},"sp":{"value":6269986320},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501869,"name":"Java: DefaultDispatcher-worker-12","threadState":{"x":[{"value":260},{"value":0},{"value":9984},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999000},{"value":6272132568},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762607584},{"value":49762607648},{"value":6272135392},{"value":999999000},{"value":59},{"value":9984},{"value":9985},{"value":10240},{"value":0},{"value":49762606080}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6272132688},"sp":{"value":6272132544},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4567738896,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501870,"name":"Java: DefaultDispatcher-worker-13","threadState":{"x":[{"value":260},{"value":0},{"value":14592},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6274278984},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762609376},{"value":49762609440},{"value":6274281696},{"value":0},{"value":60},{"value":14592},{"value":14593},{"value":14848},{"value":20757713864},{"value":49762607872}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6274279104},"sp":{"value":6274278960},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571996880,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501871,"name":"Java: DefaultDispatcher-worker-14","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6276425256},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762611168},{"value":49762611232},{"value":6276428000},{"value":0},{"value":60},{"value":512},{"value":513},{"value":768},{"value":0},{"value":49762609664}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6276425376},"sp":{"value":6276425232},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501872,"name":"Java: DefaultDispatcher-worker-15","threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999000},{"value":6278571560},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762612960},{"value":49762613024},{"value":6278574304},{"value":999999000},{"value":59},{"value":0},{"value":1},{"value":256},{"value":0},{"value":49762611456}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6278571680},"sp":{"value":6278571536},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501873,"name":"Java: DefaultDispatcher-worker-16","threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":60},{"value":0},{"value":6280717864},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49762614752},{"value":49762614816},{"value":6280720608},{"value":0},{"value":60},{"value":0},{"value":257},{"value":512},{"value":0},{"value":49762613248}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6280717984},"sp":{"value":6280717840},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501874,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1792},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998083},{"value":6282863256},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777396960},{"value":49777397024},{"value":6282866912},{"value":999998083},{"value":59},{"value":1792},{"value":1793},{"value":2048},{"value":20867392696},{"value":49777395456}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6282863376},"sp":{"value":6282863232},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501875,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999993792},{"value":6285009560},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777398752},{"value":49777398816},{"value":6285013216},{"value":999993792},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":18445509237964872210},{"value":49777397248}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6285009680},"sp":{"value":6285009536},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501876,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998417},{"value":6287155784},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777400544},{"value":49777400608},{"value":6287159520},{"value":999998417},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":21004565152},{"value":49777399040}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6287155904},"sp":{"value":6287155760},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501877,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996667},{"value":6289302216},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777402336},{"value":49777402400},{"value":6289305824},{"value":999996667},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":2598466695},{"value":49777400832}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6289302336},"sp":{"value":6289302192},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501878,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1792},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997583},{"value":6291448520},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777404128},{"value":49777404192},{"value":6291452128},{"value":999997583},{"value":59},{"value":1792},{"value":1793},{"value":2048},{"value":20859256904},{"value":49777402624}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6291448640},"sp":{"value":6291448496},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501879,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1536},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996917},{"value":6293594776},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777405920},{"value":49777405984},{"value":6293598432},{"value":999996917},{"value":59},{"value":1536},{"value":1537},{"value":1792},{"value":20878422624},{"value":49777404416}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6293594896},"sp":{"value":6293594752},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501880,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":2048},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998792},{"value":6295741080},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777407712},{"value":49777407776},{"value":6295744736},{"value":999998792},{"value":59},{"value":2048},{"value":2049},{"value":2304},{"value":20869270328},{"value":49777406208}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6295741200},"sp":{"value":6295741056},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501881,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997292},{"value":6297887384},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777409504},{"value":49777409568},{"value":6297891040},{"value":999997292},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":20870763592},{"value":49777408000}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6297887504},"sp":{"value":6297887360},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501882,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1280},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996917},{"value":6300033736},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777542624},{"value":49777542688},{"value":6300037344},{"value":999996917},{"value":59},{"value":1280},{"value":1281},{"value":1536},{"value":20896800560},{"value":49777541120}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6300033856},"sp":{"value":6300033712},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501883,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998250},{"value":6302179992},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777544416},{"value":49777544480},{"value":6302183648},{"value":999998250},{"value":59},{"value":512},{"value":513},{"value":768},{"value":21003612704},{"value":49777542912}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6302180112},"sp":{"value":6302179968},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501884,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999988875},{"value":6304326344},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777546208},{"value":49777546272},{"value":6304329952},{"value":999988875},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":290537011137675},{"value":49777544704}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6304326464},"sp":{"value":6304326320},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501885,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996459},{"value":6306472648},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777548000},{"value":49777548064},{"value":6306476256},{"value":999996459},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":20861326232},{"value":49777546496}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6306472768},"sp":{"value":6306472624},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501886,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996875},{"value":6308618904},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777549792},{"value":49777549856},{"value":6308622560},{"value":999996875},{"value":59},{"value":512},{"value":513},{"value":768},{"value":21003612704},{"value":49777548288}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6308619024},"sp":{"value":6308618880},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501887,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997958},{"value":6310765256},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777551584},{"value":49777551648},{"value":6310768864},{"value":999997958},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":18446331690998339978},{"value":49777550080}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6310765376},"sp":{"value":6310765232},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501888,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1536},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998375},{"value":6312911432},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777553376},{"value":49777553440},{"value":6312915168},{"value":999998375},{"value":59},{"value":1536},{"value":1537},{"value":1792},{"value":20875550560},{"value":49777551872}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6312911552},"sp":{"value":6312911408},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501889,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":1792},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996958},{"value":6315057864},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777555168},{"value":49777555232},{"value":6315061472},{"value":999996958},{"value":59},{"value":1792},{"value":1793},{"value":2048},{"value":20861370968},{"value":49777553664}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6315057984},"sp":{"value":6315057840},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501893,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":6912},{"value":0},{"value":0},{"value":160},{"value":291},{"value":884999000},{"value":6317203928},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49870522152},{"value":49870522216},{"value":6317207776},{"value":884999000},{"value":291},{"value":6912},{"value":6913},{"value":7168},{"value":49870479808},{"value":2}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6317204048},"sp":{"value":6317203904},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8620240,"symbol":"PlatformEvent::park_nanos(long)","symbolLocation":332,"imageIndex":2},{"imageOffset":8479028,"symbol":"ObjectMonitor::wait(long, bool, JavaThread*)","symbolLocation":1308,"imageIndex":2},{"imageOffset":10307172,"symbol":"ObjectSynchronizer::wait(Handle, long, JavaThread*)","symbolLocation":292,"imageIndex":2},{"imageOffset":5737308,"symbol":"JVM_MonitorWait","symbolLocation":440,"imageIndex":2},{"imageOffset":4689438812,"imageIndex":31},{"imageOffset":4566855872,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501894,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997917},{"value":6319350472},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49763097312},{"value":49763097376},{"value":6319354080},{"value":999997917},{"value":59},{"value":512},{"value":513},{"value":768},{"value":0},{"value":49763095808}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6319350592},"sp":{"value":6319350448},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501895,"name":"Java: OkHttp 9to5linux.com","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20771515936},{"value":48},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":204},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2597876091},{"value":4949984664},{"value":15},{"value":20783026072},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49763098560},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6321495296},{"value":15},{"value":4949924408},{"value":20831225816},{"value":49763097600}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6321494960},"sp":{"value":6321494912},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4569154820,"imageIndex":31},{"imageOffset":4569048028,"imageIndex":31},{"imageOffset":472452759657,"imageIndex":31}]},{"id":501896,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":1280},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997625},{"value":6323643080},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49763100896},{"value":49763100960},{"value":6323646688},{"value":999997625},{"value":59},{"value":1280},{"value":1281},{"value":1536},{"value":20871105832},{"value":49763099392}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6323643200},"sp":{"value":6323643056},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501897,"name":"Java: OkHttp www.basketuniverso.it","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20771595400},{"value":66},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":210},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2597246897},{"value":4949984664},{"value":15},{"value":20758945720},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49763102144},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6325787808},{"value":15},{"value":4949924408},{"value":20841576016},{"value":49763101184}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6325787472},"sp":{"value":6325787424},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4569154820,"imageIndex":31},{"imageOffset":4569048028,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501898,"name":"Java: OkHttp www.macstories.net","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20758926176},{"value":72},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":226},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2597409185},{"value":4949984664},{"value":15},{"value":20758927064},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49763103936},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6327934112},{"value":15},{"value":4949924408},{"value":20825861096},{"value":49763102976}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6327933776},"sp":{"value":6327933728},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4569154820,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501900,"name":"Java: OkHttp photos5.appleinsider.com","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20811517208},{"value":78},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":208},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2597447943},{"value":4949984664},{"value":15},{"value":20809089856},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49763105728},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6330080416},{"value":15},{"value":4949924408},{"value":20804051560},{"value":49763104768}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6330080080},"sp":{"value":6330080032},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4569154820,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501901,"name":"Java: OkHttp www.pianetabasket.com","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20854944792},{"value":90},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":206},{"value":18446744073709551615},{"value":4294910291},{"value":4927728600},{"value":2597268356},{"value":20765554800},{"value":15},{"value":20852033152},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49777804224},{"value":18446744073709551615},{"value":1},{"value":4960312511},{"value":16},{"value":6332229304},{"value":15},{"value":4960330848},{"value":20850460984},{"value":49777803264}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6332226464},"sp":{"value":6332226416},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4575631692,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501902,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":2304},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998291},{"value":6334374552},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777806560},{"value":49777806624},{"value":6334378208},{"value":999998291},{"value":59},{"value":2304},{"value":2305},{"value":2560},{"value":20883112784},{"value":49777805056}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6334374672},"sp":{"value":6334374528},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501903,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996500},{"value":6336520904},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49777808352},{"value":49777808416},{"value":6336524512},{"value":999996500},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":0},{"value":49777806848}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6336521024},"sp":{"value":6336520880},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501904,"name":"Java: OkHttp berlinomagazine.com","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20771665464},{"value":50},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":211},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2597949085},{"value":4949984664},{"value":15},{"value":20782922088},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49777814976},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6338665632},{"value":15},{"value":4949924408},{"value":20824764192},{"value":49777814016}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6338665296},"sp":{"value":6338665248},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4569154820,"imageIndex":31},{"imageOffset":4569048028,"imageIndex":31},{"imageOffset":433799430255,"imageIndex":31}]},{"id":501912,"name":"Java: Okio Watchdog","threadState":{"x":[{"value":260},{"value":0},{"value":35840},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999917},{"value":6340814344},{"value":0},{"value":512},{"value":2199023256066},{"value":2199023256066},{"value":512},{"value":0},{"value":2199023256064},{"value":305},{"value":8324043672},{"value":0},{"value":49753463520},{"value":49753463584},{"value":6340817120},{"value":999999917},{"value":59},{"value":35840},{"value":35841},{"value":36096},{"value":0},{"value":49753462016}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6340814464},"sp":{"value":6340814320},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4574993932,"imageIndex":31},{"imageOffset":4681258236,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501913,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":2304},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999875},{"value":6342959480},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49753467104},{"value":49753467168},{"value":6342963424},{"value":999999875},{"value":59},{"value":2304},{"value":2305},{"value":2560},{"value":2601439692},{"value":49753465600}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6342959600},"sp":{"value":6342959456},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501914,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":2048},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998875},{"value":6345105784},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49753468896},{"value":49753468960},{"value":6345109728},{"value":999998875},{"value":59},{"value":2048},{"value":2049},{"value":2304},{"value":0},{"value":49753467392}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6345105904},"sp":{"value":6345105760},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501915,"name":"Java: OkHttp sifted.eu","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":21018771712},{"value":48},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":224},{"value":18446744073709551615},{"value":4294910291},{"value":20771700856},{"value":2598017603},{"value":4949984664},{"value":15},{"value":20891650584},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49753470144},{"value":18446744073709551615},{"value":1},{"value":4949903282},{"value":16},{"value":6347251240},{"value":15},{"value":4949924408},{"value":20855040400},{"value":49753469184}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6347250912},"sp":{"value":6347250864},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4566607736,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257036,"imageIndex":31},{"imageOffset":4689650020,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501916,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":2048},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999993791},{"value":6349398680},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49753472480},{"value":49753472544},{"value":6349402336},{"value":999993791},{"value":59},{"value":2048},{"value":2049},{"value":2304},{"value":20874130048},{"value":49753470976}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6349398800},"sp":{"value":6349398656},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501919,"name":"Java: OkHttp net-storage.tcccdn.com","threadState":{"x":[{"value":4},{"value":0},{"value":18446744073709551615},{"value":20819124816},{"value":80},{"value":12},{"value":4412477440,"symbolLocation":0,"symbol":"ParCompactionManager::_objarray_task_queues"},{"value":207},{"value":18446744073709551615},{"value":4294910291},{"value":4927728600},{"value":2594868620},{"value":20765554800},{"value":15},{"value":20809430424},{"value":687215936032},{"value":230},{"value":4729061092055328640},{"value":0},{"value":49754264000},{"value":18446744073709551615},{"value":1},{"value":4960312511},{"value":16},{"value":6352119480},{"value":15},{"value":4960330848},{"value":20802421248},{"value":49754263040}],"flavor":"ARM_THREAD_STATE64","lr":{"value":4302939036},"cpsr":{"value":2684358656},"fp":{"value":6352116640},"sp":{"value":6352116592},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478542528},"far":{"value":0}},"frames":[{"imageOffset":38592,"symbol":"poll","symbolLocation":8,"imageIndex":25},{"imageOffset":25500,"symbol":"Java_sun_nio_ch_Net_poll","symbolLocation":80,"imageIndex":5},{"imageOffset":4689639928,"imageIndex":31},{"imageOffset":4575631692,"imageIndex":31},{"imageOffset":21488136,"imageIndex":31}]},{"id":501920,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":1280},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997000},{"value":6354264776},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49754273504},{"value":49754273568},{"value":6354268384},{"value":999997000},{"value":59},{"value":1280},{"value":1281},{"value":1536},{"value":20879084928},{"value":49754272000}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6354264896},"sp":{"value":6354264752},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501921,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998333},{"value":6356411080},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49749316576},{"value":49749316640},{"value":6356414688},{"value":999998333},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":20867443968},{"value":49749315072}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6356411200},"sp":{"value":6356411056},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501922,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":1024},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999993375},{"value":6358557336},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49749318368},{"value":49749318432},{"value":6358560992},{"value":999993375},{"value":59},{"value":1024},{"value":1025},{"value":1280},{"value":20894705672},{"value":49749316864}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6358557456},"sp":{"value":6358557312},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501923,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997791},{"value":6360703688},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49749500384},{"value":49749500448},{"value":6360707296},{"value":999997791},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":0},{"value":49749498880}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6360703808},"sp":{"value":6360703664},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4568509496,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501925,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999292},{"value":6362849944},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49750048224},{"value":49750048288},{"value":6362853600},{"value":999999292},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":20874669568},{"value":49750046720}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6362850064},"sp":{"value":6362849920},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":501926,"name":"Java: OkHttp TaskRunner","threadState":{"x":[{"value":260},{"value":0},{"value":256},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999997791},{"value":6364996248},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49750328800},{"value":49750328864},{"value":6364999904},{"value":999997791},{"value":59},{"value":256},{"value":257},{"value":512},{"value":0},{"value":49750327296}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6364996368},"sp":{"value":6364996224},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4681271944,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502008,"frames":[],"threadState":{"x":[{"value":13526429696},{"value":143139},{"value":13525893120},{"value":0},{"value":409604},{"value":18446744073709551615},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":0},"cpsr":{"value":4096},"fp":{"value":0},"sp":{"value":13526429696},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478760852},"far":{"value":0}}},{"id":502010,"name":"CVDisplayLink","threadState":{"x":[{"value":316},{"value":0},{"value":0},{"value":0},{"value":0},{"value":65704},{"value":0},{"value":7874958},{"value":198401},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49851982904},{"value":49851982968},{"value":1},{"value":7874958},{"value":0},{"value":0},{"value":198401},{"value":198656},{"value":333909472159},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782728},"cpsr":{"value":2684358656},"fp":{"value":13543517616},"sp":{"value":13543517472},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28936,"symbol":"_pthread_cond_wait","symbolLocation":1028,"imageIndex":32},{"imageOffset":15164,"symbol":"CVDisplayLink::waitUntil(unsigned long long)","symbolLocation":336,"imageIndex":40},{"imageOffset":11300,"symbol":"CVDisplayLink::runIOThread()","symbolLocation":500,"imageIndex":40},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502011,"name":"CVDisplayLink","threadState":{"x":[{"value":316},{"value":0},{"value":0},{"value":0},{"value":0},{"value":65704},{"value":0},{"value":7412625},{"value":198401},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49851985976},{"value":49851986040},{"value":1},{"value":7412625},{"value":0},{"value":0},{"value":198401},{"value":198656},{"value":333909472163},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782728},"cpsr":{"value":2684358656},"fp":{"value":13544091056},"sp":{"value":13544090912},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28936,"symbol":"_pthread_cond_wait","symbolLocation":1028,"imageIndex":32},{"imageOffset":15164,"symbol":"CVDisplayLink::waitUntil(unsigned long long)","symbolLocation":336,"imageIndex":40},{"imageOffset":11300,"symbol":"CVDisplayLink::runIOThread()","symbolLocation":500,"imageIndex":40},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502062,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":768},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999875},{"value":13436297816},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49707564512},{"value":49707564576},{"value":13436301536},{"value":999999875},{"value":59},{"value":768},{"value":769},{"value":1024},{"value":20809706496},{"value":49707563008}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":13436297936},"sp":{"value":13436297792},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4574695460,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502063,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":256},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999999167},{"value":13438444120},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49719969248},{"value":49719969312},{"value":13438447840},{"value":999999167},{"value":59},{"value":256},{"value":257},{"value":512},{"value":20809937864},{"value":49719967744}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":13438444240},"sp":{"value":13438444096},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4574695460,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502064,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999998916},{"value":13440590424},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49719971040},{"value":49719971104},{"value":13440594144},{"value":999998916},{"value":59},{"value":512},{"value":513},{"value":768},{"value":20809446632},{"value":49719969536}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":13440590544},"sp":{"value":13440590400},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4574695460,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502065,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996292},{"value":13442736728},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49719974624},{"value":49719974688},{"value":13442740448},{"value":999996292},{"value":59},{"value":512},{"value":513},{"value":768},{"value":20809110648},{"value":49719973120}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":13442736848},"sp":{"value":13442736704},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4574695460,"imageIndex":31},{"imageOffset":4681258016,"imageIndex":31},{"imageOffset":4681256816,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502173,"name":"Java: OkHttp Dispatcher","threadState":{"x":[{"value":260},{"value":0},{"value":512},{"value":0},{"value":0},{"value":160},{"value":59},{"value":999996292},{"value":13444882808},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49720595424},{"value":49720595488},{"value":13444886752},{"value":999996292},{"value":59},{"value":512},{"value":513},{"value":768},{"value":20852053696},{"value":49720593920}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":13444882928},"sp":{"value":13444882784},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":8621260,"symbol":"Parker::park(bool, long)","symbolLocation":492,"imageIndex":2},{"imageOffset":10724820,"symbol":"Unsafe_Park(JNIEnv_*, _jobject*, unsigned char, long)","symbolLocation":328,"imageIndex":2},{"imageOffset":4689442140,"imageIndex":31},{"imageOffset":4571248844,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681258540,"imageIndex":31},{"imageOffset":4681257340,"imageIndex":31},{"imageOffset":4681236800,"imageIndex":31},{"imageOffset":4957572,"symbol":"JavaCalls::call_helper(JavaValue*, methodHandle const&, JavaCallArguments*, JavaThread*)","symbolLocation":992,"imageIndex":2},{"imageOffset":4953268,"symbol":"JavaCalls::call_virtual(JavaValue*, Klass*, Symbol*, Symbol*, JavaCallArguments*, JavaThread*)","symbolLocation":320,"imageIndex":2},{"imageOffset":4953472,"symbol":"JavaCalls::call_virtual(JavaValue*, Handle, Klass*, Symbol*, Symbol*, JavaThread*)","symbolLocation":100,"imageIndex":2},{"imageOffset":5817732,"symbol":"thread_entry(JavaThread*, JavaThread*)","symbolLocation":156,"imageIndex":2},{"imageOffset":5040360,"symbol":"JavaThread::thread_main_inner()","symbolLocation":152,"imageIndex":2},{"imageOffset":10502724,"symbol":"Thread::call_run()","symbolLocation":200,"imageIndex":2},{"imageOffset":8589200,"symbol":"thread_native_entry(Thread*)","symbolLocation":280,"imageIndex":2},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502184,"name":"GC Timer thread","threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":9},{"value":999858292},{"value":6186561112},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49886393000},{"value":49886393064},{"value":6186561760},{"value":999858292},{"value":9},{"value":0},{"value":1},{"value":256},{"value":18437520701672696841},{"value":1}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6186561232},"sp":{"value":6186561088},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":141284,"symbol":"std::__1::condition_variable::__do_timed_wait(std::__1::unique_lock<std::__1::mutex>&, std::__1::chrono::time_point<std::__1::chrono::system_clock, std::__1::chrono::duration<long long, std::__1::ratio<1l, 1000000000l>>>)","symbolLocation":104,"imageIndex":35},{"imageOffset":237020,"symbol":"void kotlin::RepeatedTimer<kotlin::steady_clock>::Run<kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()>(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&)","symbolLocation":256,"imageIndex":19},{"imageOffset":237932,"symbol":"void* std::__1::__thread_proxy[abi:ne200100]<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct>>, void (*)(kotlin::ScopedThread::attributes, void (*&&)(void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), kotlin::ScopedThread::attributes, void (*)(void (kotlin::RepeatedTimer<kotlin::steady_clock>::*&&)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*&&, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&), void (kotlin::RepeatedTimer<kotlin::steady_clock>::*)(kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()&&) noexcept, kotlin::RepeatedTimer<kotlin::steady_clock>*, kotlin::gcScheduler::internal::GCSchedulerDataAdaptive<kotlin::steady_clock>::GCSchedulerDataAdaptive(kotlin::gcScheduler::GCSchedulerConfig&, std::__1::function<long long ()>)::'lambda'()>>(void*)","symbolLocation":124,"imageIndex":19},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502185,"name":"Main GC thread","threadState":{"x":[{"value":260},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":0},{"value":0},{"value":6243085992},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":49835147264},{"value":49835147472},{"value":6243086560},{"value":0},{"value":0},{"value":0},{"value":1},{"value":256},{"value":0},{"value":0}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6243086112},"sp":{"value":6243085968},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":141132,"symbol":"std::__1::condition_variable::wait(std::__1::unique_lock<std::__1::mutex>&)","symbolLocation":32,"imageIndex":35},{"imageOffset":218772,"symbol":"std::__1::invoke_result<kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>::type kotlin::UtilityThread::Run<kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&)","symbolLocation":212,"imageIndex":19},{"imageOffset":219092,"symbol":"void* std::__1::__thread_proxy[abi:ne200100]<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct>>, void (*)(kotlin::ScopedThread::attributes, void (*&&)(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::ScopedThread::attributes, void (*)(kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()&&), kotlin::gc::internal::MainGCThread<kotlin::gc::internal::PmcsGCTraits>::MainGCThread(GCStateHolder&, kotlin::alloc::Allocator&, kotlin::gcScheduler::GCScheduler&, kotlin::gc::mark::ParallelMark&)::'lambda'()>>(void*)","symbolLocation":116,"imageIndex":19},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]},{"id":502186,"name":"JavaScriptCore libpas scavenger","threadState":{"x":[{"value":316},{"value":0},{"value":0},{"value":0},{"value":0},{"value":160},{"value":0},{"value":99998968},{"value":6247952040},{"value":0},{"value":0},{"value":2},{"value":2},{"value":0},{"value":0},{"value":0},{"value":305},{"value":8324043672},{"value":0},{"value":13586114624},{"value":13586114688},{"value":6247952608},{"value":99998968},{"value":0},{"value":0},{"value":1},{"value":256},{"value":8274157568,"symbolLocation":2744,"symbol":"WTF::RefLogSingleton::s_buffer"},{"value":8303661056,"symbolLocation":1816,"symbol":"bmalloc_common_primitive_heap_support"}],"flavor":"ARM_THREAD_STATE64","lr":{"value":6478782684},"cpsr":{"value":1610616832},"fp":{"value":6247952160},"sp":{"value":6247952016},"esr":{"value":1442840704,"description":"(Syscall)"},"pc":{"value":6478521592},"far":{"value":0}},"frames":[{"imageOffset":17656,"symbol":"__psynch_cvwait","symbolLocation":8,"imageIndex":25},{"imageOffset":28892,"symbol":"_pthread_cond_wait","symbolLocation":984,"imageIndex":32},{"imageOffset":26205816,"symbol":"scavenger_thread_main","symbolLocation":1440,"imageIndex":41},{"imageOffset":27656,"symbol":"_pthread_start","symbolLocation":136,"imageIndex":32},{"imageOffset":7080,"symbol":"thread_start","symbolLocation":8,"imageIndex":32}]}],
"usedImages" : [
{
"source" : "P",
"arch" : "arm64",
"base" : 4299177984,
"size" : 16384,
"uuid" : "4acdb266-eb95-3273-bb57-c2e40a559c3e",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/bin\/java",
"name" : "java"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4299440128,
"size" : 81920,
"uuid" : "d08d7a99-f8f9-34f3-aea6-58bd8b3d1f99",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libjli.dylib",
"name" : "libjli.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4398514176,
"size" : 12730368,
"uuid" : "8740592f-ba05-3ed9-8468-4ee832bedc5e",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/server\/libjvm.dylib",
"name" : "libjvm.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4299603968,
"size" : 16384,
"uuid" : "0383fb59-81f4-3bbf-9741-c22701a96b4b",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libjimage.dylib",
"name" : "libjimage.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4300144640,
"size" : 81920,
"uuid" : "50a1bc12-5947-33f5-86d2-c745235440c1",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libjava.dylib",
"name" : "libjava.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4302913536,
"size" : 49152,
"uuid" : "ab4bb1dd-c841-3698-8d60-0188a71b9c00",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libnio.dylib",
"name" : "libnio.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4303044608,
"size" : 49152,
"uuid" : "8449cb27-edb4-34ae-816e-e7d249c44f4e",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libnet.dylib",
"name" : "libnet.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4303323136,
"size" : 98304,
"uuid" : "c38326d7-e933-390a-b6fa-57038e37c0ee",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libzip.dylib",
"name" : "libzip.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5037916160,
"size" : 18120704,
"uuid" : "06e3c1c6-f227-3754-b778-2700d14f03df",
"path" : "\/Users\/USER\/*\/libskiko-macos-arm64.dylib",
"name" : "libskiko-macos-arm64.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4397645824,
"size" : 409600,
"uuid" : "8199d7eb-a961-3e8b-b598-76fd218d6619",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libawt.dylib",
"name" : "libawt.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4910661632,
"size" : 409600,
"uuid" : "83560f65-7886-32da-8d93-9966a4742be2",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libmlib_image.dylib",
"name" : "libmlib_image.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4912365568,
"size" : 753664,
"uuid" : "087fdeb9-bd42-33c7-91f7-e58eee343ab0",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libawt_lwawt.dylib",
"name" : "libawt_lwawt.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4397072384,
"size" : 32768,
"uuid" : "7ff3a0f1-7030-3932-bc0b-b6607c38df4a",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libosxapp.dylib",
"name" : "libosxapp.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5019238400,
"size" : 1720320,
"uuid" : "35779de6-1933-3b15-9725-f0a8b04e181d",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libfontmanager.dylib",
"name" : "libfontmanager.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 4913610752,
"size" : 507904,
"uuid" : "57965598-6495-31f9-9f38-4bdf1a5a0885",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libfreetype.dylib",
"name" : "libfreetype.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 4397416448,
"size" : 49152,
"uuid" : "f8bd9069-8c4f-37ea-af9a-2b1060f54e4f",
"path" : "\/usr\/lib\/libobjc-trampolines.dylib",
"name" : "libobjc-trampolines.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 5280825344,
"CFBundleShortVersionString" : "341.11",
"CFBundleIdentifier" : "com.apple.AGXMetalG13X",
"size" : 7667712,
"uuid" : "f4b41620-c555-31a5-ae52-eca9b80bf0d8",
"path" : "\/System\/Library\/Extensions\/AGXMetalG13X.bundle\/Contents\/MacOS\/AGXMetalG13X",
"name" : "AGXMetalG13X",
"CFBundleVersion" : "341.11"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5019090944,
"size" : 32768,
"uuid" : "58115166-58e0-3807-b591-807407765146",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libosxui.dylib",
"name" : "libosxui.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5263556608,
"size" : 360448,
"uuid" : "3d70fa62-b878-3d61-818c-c887c8a1436d",
"path" : "\/Users\/USER\/*\/libikloud.dylib",
"name" : "libikloud.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5265211392,
"size" : 491520,
"uuid" : "68b3c35d-923b-3973-b3f0-1062814470f3",
"path" : "\/Users\/USER\/*\/libwebview.dylib",
"name" : "libwebview.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5264769024,
"size" : 16384,
"uuid" : "2bc56aa7-41f2-38e0-8c74-f6af7fb3bca7",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libjawt.dylib",
"name" : "libjawt.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5264998400,
"size" : 114688,
"uuid" : "624d6700-8917-3027-87d7-871a6cd08ed5",
"path" : "\/Users\/USER\/Library\/Caches\/*\/jna3417252500363705797.tmp",
"name" : "jna3417252500363705797.tmp"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5280514048,
"size" : 16384,
"uuid" : "1e9150d9-86d6-3570-ba22-f90b792a62cc",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libprefs.dylib",
"name" : "libprefs.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5307777024,
"size" : 1032192,
"uuid" : "86cd20e1-38ec-32e5-b772-8f07d8f09077",
"path" : "\/private\/var\/folders\/*\/sqlite-3.49.1.0-25948a9c-20ed-4cd2-b495-bb93b6d013c1-libsqlitejdbc.dylib",
"name" : "sqlite-3.49.1.0-25948a9c-20ed-4cd2-b495-bb93b6d013c1-libsqlitejdbc.dylib"
},
{
"source" : "P",
"arch" : "arm64",
"base" : 5307334656,
"size" : 16384,
"uuid" : "62c7d081-4966-3c2c-9fea-caa4c8a66bbe",
"path" : "\/Library\/Java\/JavaVirtualMachines\/zulu-21.jdk\/Contents\/Home\/lib\/libextnet.dylib",
"name" : "libextnet.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6478503936,
"size" : 246944,
"uuid" : "9fe7c84d-0c2b-363f-bee5-6fd76d67a227",
"path" : "\/usr\/lib\/system\/libsystem_kernel.dylib",
"name" : "libsystem_kernel.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6479040512,
"CFBundleShortVersionString" : "6.9",
"CFBundleIdentifier" : "com.apple.CoreFoundation",
"size" : 5532352,
"uuid" : "3c4a3add-9e48-33da-82ee-80520e6cbe3b",
"path" : "\/System\/Library\/Frameworks\/CoreFoundation.framework\/Versions\/A\/CoreFoundation",
"name" : "CoreFoundation",
"CFBundleVersion" : "4109.1.401"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6691221504,
"CFBundleShortVersionString" : "2.1.1",
"CFBundleIdentifier" : "com.apple.HIToolbox",
"size" : 3155968,
"uuid" : "9ab64c08-0685-3a0d-9a7e-83e7a1e9ebb4",
"path" : "\/System\/Library\/Frameworks\/Carbon.framework\/Versions\/A\/Frameworks\/HIToolbox.framework\/Versions\/A\/HIToolbox",
"name" : "HIToolbox"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6550642688,
"CFBundleShortVersionString" : "6.9",
"CFBundleIdentifier" : "com.apple.AppKit",
"size" : 24300448,
"uuid" : "3c0949bb-e361-369a-80b7-17440eb09e98",
"path" : "\/System\/Library\/Frameworks\/AppKit.framework\/Versions\/C\/AppKit",
"name" : "AppKit",
"CFBundleVersion" : "2685.20.119"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6504509440,
"CFBundleShortVersionString" : "6.9",
"CFBundleIdentifier" : "com.apple.Foundation",
"size" : 16396896,
"uuid" : "00467f1f-216a-36fe-8587-c820c7e0437d",
"path" : "\/System\/Library\/Frameworks\/Foundation.framework\/Versions\/C\/Foundation",
"name" : "Foundation",
"CFBundleVersion" : "4109.1.401"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6474780672,
"size" : 651108,
"uuid" : "b50f5a1a-be81-3068-92e1-3554f2be478a",
"path" : "\/usr\/lib\/dyld",
"name" : "dyld"
},
{
"size" : 0,
"source" : "A",
"base" : 0,
"uuid" : "00000000-0000-0000-0000-000000000000"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6478753792,
"size" : 51900,
"uuid" : "e95973b8-824c-361e-adf4-390747c40897",
"path" : "\/usr\/lib\/system\/libsystem_pthread.dylib",
"name" : "libsystem_pthread.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6474579968,
"size" : 199628,
"uuid" : "a64128d9-375b-36ae-8feb-69d80f987d3c",
"path" : "\/usr\/lib\/system\/libdyld.dylib",
"name" : "libdyld.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 7171571712,
"CFBundleShortVersionString" : "21622",
"CFBundleIdentifier" : "com.apple.WebKit",
"size" : 22128832,
"uuid" : "3b55482a-efe2-35a7-b1c9-3f41a823a30b",
"path" : "\/System\/Library\/Frameworks\/WebKit.framework\/Versions\/A\/WebKit",
"name" : "WebKit",
"CFBundleVersion" : "21622.2.11.11.9"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6477791232,
"size" : 601684,
"uuid" : "b29f2164-26b0-3016-a871-82de5a4637ff",
"path" : "\/usr\/lib\/libc++.1.dylib",
"name" : "libc++.1.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 7328763904,
"size" : 109268,
"uuid" : "b8c7c1aa-5203-3bb5-8760-a7870b239d36",
"path" : "\/System\/Library\/Frameworks\/Accelerate.framework\/Versions\/A\/Frameworks\/vImage.framework\/Versions\/A\/Libraries\/libCGInterfaces.dylib",
"name" : "libCGInterfaces.dylib"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6593482752,
"CFBundleShortVersionString" : "2.0",
"CFBundleIdentifier" : "com.apple.CoreGraphics",
"size" : 7687712,
"uuid" : "a5317723-cc87-3367-b3ae-fd7b0ea01333",
"path" : "\/System\/Library\/Frameworks\/CoreGraphics.framework\/Versions\/A\/CoreGraphics",
"name" : "CoreGraphics",
"CFBundleVersion" : "1965.1.4"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6527905792,
"CFBundleShortVersionString" : "877.1.0.5",
"CFBundleIdentifier" : "com.apple.CoreText",
"size" : 2279008,
"uuid" : "db7e0880-8f10-3512-b6ef-3c35511ce208",
"path" : "\/System\/Library\/Frameworks\/CoreText.framework\/Versions\/A\/CoreText",
"name" : "CoreText",
"CFBundleVersion" : "877.1.0.5"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6639263744,
"CFBundleShortVersionString" : "8.1",
"CFBundleIdentifier" : "com.apple.vImage",
"size" : 4416120,
"uuid" : "90793c47-d770-3ead-a12a-b1696a2b16b8",
"path" : "\/System\/Library\/Frameworks\/Accelerate.framework\/Versions\/A\/Frameworks\/vImage.framework\/Versions\/A\/vImage",
"name" : "vImage"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 6652895232,
"CFBundleShortVersionString" : "1.8",
"CFBundleIdentifier" : "com.apple.CoreVideo",
"size" : 540672,
"uuid" : "d8605842-8c6c-36d7-820d-2132d91e0c06",
"path" : "\/System\/Library\/Frameworks\/CoreVideo.framework\/Versions\/A\/CoreVideo",
"name" : "CoreVideo",
"CFBundleVersion" : "726.2"
},
{
"source" : "P",
"arch" : "arm64e",
"base" : 7036379136,
"CFBundleShortVersionString" : "21622",
"CFBundleIdentifier" : "com.apple.JavaScriptCore",
"size" : 28438144,
"uuid" : "c79071c9-db50-3264-a316-94abd0d3b9a9",
"path" : "\/System\/Library\/Frameworks\/JavaScriptCore.framework\/Versions\/A\/JavaScriptCore",
"name" : "JavaScriptCore",
"CFBundleVersion" : "21622.2.11.11.9"
}
],
"sharedCache" : {
"base" : 6473695232,
"size" : 5609635840,
"uuid" : "b69ff43c-dbfd-3fb1-b4fe-a8fe32ea1062"
},
"legacyInfo" : {
"threadTriggered" : {
"name" : "Java: AWT-EventQueue-0"
}
},
"logWritingSignature" : "563dd9f1b99dd67847cf25dcebb03edbe403a064",
"trialInfo" : {
"rollouts" : [
{
"rolloutId" : "60da5e84ab0ca017dace9abf",
"factorPackIds" : [

      ],
      "deploymentId" : 240000008
    },
    {
      "rolloutId" : "63f9578e238e7b23a1f3030a",
      "factorPackIds" : [

      ],
      "deploymentId" : 240000005
    }
],
"experiments" : [

]
}
}

Model: MacBookPro18,3, BootROM 13822.41.1, proc 8:6:2 processors, 16 GB, SMC
Graphics: Apple M1 Pro, Apple M1 Pro, Built-In
Display: Color LCD, 3024 x 1964 Retina, Main, MirrorOff, Online
Memory Module: LPDDR5, Samsung
AirPort: spairport_wireless_card_type_wifi (0x14E4, 0x4387), wl0: Sep 22 2025 22:41:34 version 20.131.4.0.8.7.215 FWID 01-45655c04
IO80211_driverkit-1530.16 "IO80211_driverkit-1530.16" Oct 10 2025 22:56:35
AirPort:
Bluetooth: Version (null), 0 services, 0 devices, 0 incoming serial ports
Network Service: Wi-Fi, AirPort, en0
Thunderbolt Bus: MacBook Pro, Apple Inc.
Thunderbolt Bus: MacBook Pro, Apple Inc.
Thunderbolt Bus: MacBook Pro, Apple Inc.
