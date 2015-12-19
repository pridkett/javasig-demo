package com.ibm.watson.watsondemo;
/**
 * Copyright 2015 IBM Corp. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classifier.Status;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classifiers;

public class NaturalLanguageClassifierTrainer {
    private Properties props;
    private NaturalLanguageClassifier nlcService;
    
    @Option(name="-l", usage="list all classifiers")
    private boolean listClassifiers;
    
    @Option(name="-s", usage="get the status of a classifier",metaVar="CLASSIFIER_ID")
    private String checkClassifierID = null;
    
    @Option(name="-d", usage="delete a classifier", metaVar="CLASSIFIER_ID")
    private String deleteClassifierID = null;
    
    @Argument
    private List<String> arguments = new ArrayList<String>();
    
    public static void main(String args[]) throws IOException {
        NaturalLanguageClassifierTrainer trainer = new NaturalLanguageClassifierTrainer();
        trainer.parseArgs(args);
        trainer.run();
    }
    
    public NaturalLanguageClassifierTrainer() {
        props = WatsonDemoProperties.props();
        
        nlcService = new NaturalLanguageClassifier();
        nlcService.setUsernameAndPassword(props.getProperty(PropNames.NLC_USERNAME), props.getProperty(PropNames.NLC_PASSWORD));
    }
    
    public void parseArgs(String args[]) throws IOException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("java NaturalLanguageClassifierTrainer [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
        }
        
        // if none of the three settings are set, then display usage and exit
        if (!listClassifiers && checkClassifierID == null && arguments.size() == 0) {
            System.err.println("java NaturalLanguageClassifierTrainer [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
        }
    }
    
    private void showClassifierList() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        
        for (Classifier c : nlcService.getClassifiers().getClassifiers()) {
            c = nlcService.getClassifier(c.getId());
            System.out.println(c.getId()+ ", " + c.getName() + ", " + 
                    df.format(c.getCreated()) + ", " + c.getStatus());
        }
    }
    
    private void checkClassifierStatus(String classifierID) {
        Classifier c = nlcService.getClassifier(classifierID);
        System.out.println(c.getStatusDescription());
    }
    
    private void trainClassifier(String name, String language, String filename) {
        File f = new File(filename);
        Classifier c = nlcService.createClassifier(name, language, f);
        System.out.println(c.getId()+ ", " + c.getName() + ", " + c.getStatus());
    }
    
    private void deleteClassifier(String classifierID) {
        nlcService.deleteClassifier(classifierID);
    }
    
    public void run() {
        if (listClassifiers) {
            showClassifierList();
        }
        
        if (checkClassifierID != null) {
            checkClassifierStatus(checkClassifierID);
        }
        
        if (deleteClassifierID != null) {
            deleteClassifier(deleteClassifierID);
        }
        
        if (arguments.size() == 3) {
            trainClassifier(arguments.get(0), arguments.get(1), arguments.get(2));
        }
    }
}
