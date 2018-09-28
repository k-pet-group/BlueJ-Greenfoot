/*
 * Copyright 2012, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * --------------------------
 * Modified for use with BlueJ.
 * 09-2012 - changed various property key names and handling of classpath.
 */

#import <Cocoa/Cocoa.h>
#include <dlfcn.h>
#include <jni.h>

#define JAVA_LAUNCH_ERROR "JavaLaunchError"

#define JVM_RUNTIME_KEY "JVMRuntime"
#define JVM_MAIN_CLASS_NAME_KEY "MainClass"

#define UNSPECIFIED_ERROR "An unknown error occurred."

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
                                    int jargc, const char** jargv,
                                    int appclassc, const char** appclassv,
                                    const char* fullversion,
                                    const char* dotversion,
                                    const char* pname,
                                    const char* lname,
                                    jboolean javaargs,
                                    jboolean cpwildcard,
                                    jboolean javaw,
                                    jint ergo);

int launch(char *);

int main(int argc, char *argv[]) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    int result;
    @try {
        launch(argv[0]);
        result = 0;
    } @catch (NSException *exception) {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setAlertStyle:NSCriticalAlertStyle];
        [alert setMessageText:[exception reason]];
        [alert runModal];

        result = 1;
    }

    [pool drain];

    return result;
}

int launch(char *commandName) {
    // Get the main bundle
    NSBundle *mainBundle = [NSBundle mainBundle];

    // Set the working directory to the user's home directory
    // chdir([NSHomeDirectory() UTF8String]);

    // Get the main bundle's info dictionary and Java dictionary
    // Note, this dictionary must not be named just "Java" - otherwise OS X 
    // insists that Java 6 be installed before allowing the application to run.
    NSDictionary *infoDictionary = [mainBundle infoDictionary];
	NSDictionary *javaDictionary = [infoDictionary objectForKey:@"JavaProps"];
	if (javaDictionary == nil) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"NoJavaDictionary", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
	}

    // Locate the JLI_Launch() function
    // NSString *runtime = [javaDictionary objectForKey:@JVM_RUNTIME_KEY];

    const char *libjliPath = NULL;
    NSString *runtimePath = nil;
        
    runtimePath = [[mainBundle bundlePath] stringByAppendingPathComponent:@"Contents/JDK"];
    libjliPath = [[runtimePath stringByAppendingPathComponent:@"Home/lib/jli/libjli.dylib"] fileSystemRepresentation];

    void *libJLI = dlopen(libjliPath, RTLD_LAZY);

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if (libJLI != NULL) {
        jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
    }
    else {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
                reason:[NSString stringWithFormat:@"%@ path %@ dlerror %@", @"JLI_Dylib_Not_Opened", [NSString stringWithUTF8String: libjliPath], [NSString stringWithUTF8String: dlerror()]]
                userInfo: nil] raise];
    }

    if (jli_LaunchFxnPtr == NULL) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"JRELoadError", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    // Get the main class name
    NSString *mainClassName = [javaDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == nil) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"MainClassNameRequired", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];
    }

    // Set the class path
    NSString *mainBundlePath = [mainBundle bundlePath];
    NSString *javaPath = [mainBundlePath stringByAppendingString:@"/Contents/Resources/Java"];
    // NSMutableString *classPath = [NSMutableString stringWithFormat:@"-Djava.class.path=%@/Classes", javaPath];
    NSMutableString *classPath = [NSMutableString stringWithString:@"-Djava.class.path="];

	NSArray *classPathEls = [javaDictionary objectForKey:@"ClassPath"];
	if (classPathEls == nil) {
        [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
            reason:NSLocalizedString(@"ClasspathRequired", @UNSPECIFIED_ERROR)
            userInfo:nil] raise];		
	}
	
	BOOL first = YES;
	for (NSString *classPathEl in classPathEls) {
		if (first == NO) {
			[classPath appendString:@":"];
		}
		if ([classPathEl hasPrefix:@"$JAVAROOT/"]) {
			[classPath appendFormat:@"%@%@", javaPath, [classPathEl substringFromIndex:9]];
		}
		else if ([classPathEl hasPrefix:@"$JVMROOT/"]) {
			[classPath appendFormat:@"%@%@", runtimePath, [classPathEl substringFromIndex:8]];
		}
		else {
			[classPath appendString:classPathEl];
		}
		first = NO;
	}
	
    //NSFileManager *defaultFileManager = [NSFileManager defaultManager];
    //NSArray *javaDirectoryContents = [defaultFileManager contentsOfDirectoryAtPath:javaPath error:nil];
    //if (javaDirectoryContents == nil) {
    //    [[NSException exceptionWithName:@JAVA_LAUNCH_ERROR
    //        reason:NSLocalizedString(@"JavaDirectoryNotFound", @UNSPECIFIED_ERROR)
    //        userInfo:nil] raise];
    //}

    //for (NSString *file in javaDirectoryContents) {
    //    if ([file hasSuffix:@".jar"]) {
    //        [classPath appendFormat:@":%@/%@", javaPath, file];
    //    }
    //}

    // Set the library path
    NSString *libraryPath = [NSString stringWithFormat:@"-Djava.library.path=%@/Contents/MacOS", mainBundlePath];
    
    // Construct the application name argument
    NSString *appNameArg = [NSString stringWithFormat:@"-Xdock:name=%@", [infoDictionary objectForKey:@"CFBundleName"]]; 
    
    // Get the VM properties
    NSDictionary *vmProps = [javaDictionary objectForKey:@"Properties"];
    if (vmProps == nil) {
        vmProps = [NSDictionary dictionary];
    }
    
    // Get the application arguments
    NSArray *appArgs = [javaDictionary objectForKey:@"Arguments"];
    if (appArgs == nil) {
    	appArgs = [NSArray array];
    }

    // Initialize the arguments to JLI_Launch()
    int argc = 3 + [vmProps count] + 1 + 1 + [appArgs count];
    char *argv[argc];

    int i = 0;
    argv[i++] = commandName;
    argv[i++] = strdup([appNameArg UTF8String]);
    argv[i++] = strdup([classPath UTF8String]);
    argv[i++] = strdup([libraryPath UTF8String]);
    
    // Declare pointers to allow access in the block below
    int * const iPtr = &i;
    char ** const argvPtr = argv;
    
    [vmProps enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
    	NSString *propString = [NSString stringWithFormat:@"-D%@=%@", key, obj];
    	argvPtr[(*iPtr)++] = strdup([propString UTF8String]);
    }];

    argv[i++] = strdup([mainClassName UTF8String]);
    
    for (id arg in appArgs) {
    	argv[i++] = strdup([arg UTF8String]);
    }

    // Invoke JLI_Launch()
    return jli_LaunchFxnPtr(argc, argv,
                            0, NULL,
                            0, NULL,
                            "",
                            "",
                            "java",
                            "java",
                            FALSE,
                            FALSE,
                            FALSE,
                            0);
}
