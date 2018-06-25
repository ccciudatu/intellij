/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.sdkcompat.clion;

import com.intellij.openapi.application.ApplicationManager;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains.DebuggerKind;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains.Toolchain;
import com.jetbrains.cidr.execution.debugger.CidrDebuggerPathManager;
import java.io.File;

/** Handles changes to toolchains between different api versions */
public class ToolchainUtils {

  private static Toolchain createDefaultToolchain() {
    Toolchain toolchain = new Toolchain(CPPToolchains.OSType.getCurrent());
    toolchain.setName(Toolchain.DEFAULT);
    return toolchain;
  }

  public static Toolchain getToolchain() {
    Toolchain toolchain = CPPToolchains.getInstance().getDefaultToolchain();
    if (toolchain == null) {
      toolchain = createDefaultToolchain();
    }
    return toolchain;
  }

  public static void setDefaultDebuggerPath(String debuggerPath) {
    ApplicationManager.getApplication()
        .runWriteAction(
            () -> {
              CPPToolchains cppToolchains = CPPToolchains.getInstance();
              Toolchain toolchain = cppToolchains.getDefaultToolchain();
              if (toolchain != null) {
                cppToolchains.beginUpdate();
                toolchain.setDebuggerKind(DebuggerKind.CUSTOM_GDB);
                toolchain.setCustomGDBExecutablePath(debuggerPath);
                cppToolchains.endUpdate();
                return;
              }

              Toolchain newToolchain = createDefaultToolchain();
              newToolchain.setDebuggerKind(DebuggerKind.CUSTOM_GDB);
              newToolchain.setCustomGDBExecutablePath(debuggerPath);
              cppToolchains.beginUpdate();
              cppToolchains.addToolchain(newToolchain);
              cppToolchains.endUpdate();
            });
  }

  public static void setDebuggerToDefault(CPPToolchains.Toolchain toolchain) {
    Toolchain defaultToolchain = getToolchain();
    toolchain.setCustomGDBExecutablePath(defaultToolchain.getCustomGDBExecutablePath());
    toolchain.setDebuggerKind(defaultToolchain.getDebuggerKind());
  }

  /**
   * Used to register an instance of this class for unit tests. This needs to be in sdkcompat
   * because CPPToolchains changed packages between 2017.2 and 2017.3. #api173
   */
  public static Class<CPPToolchains> getCppToolchainsClass() {
    return CPPToolchains.class;
  }

  /**
   * Used to register an instance of this class for unit tests. This needs to be in sdkcompat
   * because CPPToolchains changed packages between 2017.2 and 2017.3. #api173
   */
  public static CPPToolchains createCppToolchainsInstance() {
    return new CPPToolchains();
  }

  /** Used to abstract away different between CPPToolchains.Toolchain #api173 */
  public static class ToolchainCompat extends Toolchain {
    public ToolchainCompat() {
      super(CPPToolchains.OSType.getCurrent());
    }
  }

  /** Get the GDB file. Return the bundled one rather than null #api173 */
  public static File getDebuggerFile(CPPToolchains.Toolchain toolchain) {
    if (toolchain.getDebuggerKind() == DebuggerKind.BUNDLED_GDB) {
      return CidrDebuggerPathManager.getBundledGDBBinary();
    }
    String gdbPath = toolchain.getCustomGDBExecutablePath();
    if (gdbPath == null) {
      return CidrDebuggerPathManager.getBundledGDBBinary();
    }
    File gdbFile = new File(gdbPath);
    if (!gdbFile.exists()) {
      return CidrDebuggerPathManager.getBundledGDBBinary();
    }
    return gdbFile;
  }
}
