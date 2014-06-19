/*
 *  Copyright 2010 GTRI
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package gdrive.cli.util;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * A PatternLayout for Log4j utilizing JANSI to perform syntax highlighting.
 * <br/><br/>
 * @author brad
 * @date Oct 22, 2010
 */
public class JANSIPatternLayout extends PatternLayout {
    //==========================================================================
    //    Private Static Methods
    //==========================================================================
    private static Boolean hasLength( String str ){
        return str != null && str.trim().length() > 0;
    }
    static {
        if( !hasLength(System.getProperty("surefire.test.class.path")) ){
            AnsiConsole.systemInstall();
        }
    }
    //==========================================================================
    //    Constructors
    //==========================================================================
    public JANSIPatternLayout(String pattern) {
        super(pattern);
    }
    public JANSIPatternLayout() {}
    //==========================================================================
    //    Protected Methods
    //==========================================================================
    protected Object ansi(Object obj){
        Ansi ansi = Ansi.ansi();
        return ansi.render(obj.toString());
    }
    protected String getColor( Level level ){
        if( Level.DEBUG.equals(level) ){
            return "cyan";
        }else if( Level.INFO.equals(level) ){
            return "white";
        }else if( Level.WARN.equals(level) ){
            return "yellow";
        }else if( Level.ERROR.equals(level) ){
            return "red";
        }else if( Level.TRACE.equals(level) ){
            return "blue";
        }else{
            return "white";
        }
    }//end getColor()
    //==========================================================================
    //    Overriden Methods
    //==========================================================================
    @Override
    public String format(LoggingEvent event) {
        String formatted = super.format(event);
        Level level = event.getLevel();
        String color = this.getColor(level);
        formatted = formatted.replace(level.toString(), "@|"+color+" "+level.toString()+"|@");
        return ansi(formatted).toString();
    }

}/* end JANSIPatternLayout */
