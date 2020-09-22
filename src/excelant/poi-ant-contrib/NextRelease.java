/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class NextRelease extends Task {
    private final Pattern pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*");
    private String property;
    private int increment = 1;

    public void setProperty(String property) {
        this.property = property;
    }

    public void increment(int increment) {
        this.increment = increment;
    }

    public void execute() {
        Project project = getProject();
        String relCurr = project.getProperty("version.id");
        Matcher m = pattern.matcher(relCurr);
        m.find();
        project.setProperty(property, m.group(1)+"."+m.group(2)+"."+(Integer.parseInt(m.group(3))+increment));
    }
}
