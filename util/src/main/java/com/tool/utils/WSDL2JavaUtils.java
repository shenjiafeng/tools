package com.tool.utils;

import org.apache.axis.utils.CLArgsParser;
import org.apache.axis.utils.CLOption;
import org.apache.axis.utils.Messages;
import org.apache.axis.wsdl.WSDL2Java;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author jiafengshen 2018/3/28.
 */
public class WSDL2JavaUtils extends WSDL2Java {

    private static Logger logger = LoggerFactory.getLogger(WSDL2JavaUtils.class);

    public void parse(String[] args) throws Throwable {

        // Parse the arguments
        CLArgsParser argsParser = new CLArgsParser(args, options);

        // Print parser errors, if any
        if (null != argsParser.getErrorString()) {
            System.err.println(
                    Messages.getMessage("error01", argsParser.getErrorString()));
            printUsage();
        }
        // Get a list of parsed options
        List clOptions = argsParser.getArguments();
        int size = clOptions.size();
        // Parse the options and configure the emitter as appropriate.
        for (int i = 0; i < size; i++) {
            parseOption((CLOption) clOptions.get(i));
        }

        // validate argument combinations
        //
        validateOptions();
        parser.run(wsdlURI);
    }

}
