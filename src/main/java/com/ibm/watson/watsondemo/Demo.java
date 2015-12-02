package com.ibm.watson.watsondemo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.IOUtils;

import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationModel;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.Classification;
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.model.ClassifiedClass;
import com.ibm.watson.developer_cloud.speech_to_text.v1.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechModel;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class Demo {
    private Properties props;
    private SpeechToText sttService;
    private TextToSpeech ttsService;
    private NaturalLanguageClassifier nlcService;
    private LanguageTranslation ltService;
    private List<Voice> ttsVoices;
    private List<SpeechModel> sttModels;
    private List<TranslationModel> ltModels;
    private String inputLanguage;
    private String outputLanguage;
    private Voice outputVoice;

    private String classifierId;
    private SpeechModel inputSpeechModel;
    private TranslationModel inputTranslationModel;
    private TranslationModel outputTranslationModel;
    
    private Boolean debug = false;

    public static void main(String[] args) {
        Demo d = new Demo();
        d.run();
    }
    
    public Demo() {
        props = WatsonDemoProperties.props();
    }
    
    public void run() {
        sttService = new SpeechToText();
        sttService.setUsernameAndPassword(props.getProperty(PropNames.STT_USERNAME), props.getProperty(PropNames.STT_PASSWORD));

        ttsService = new TextToSpeech();
        ttsService.setUsernameAndPassword(props.getProperty(PropNames.TTS_USERNAME), props.getProperty(PropNames.TTS_PASSWORD));

        ltService = new LanguageTranslation();
        ltService.setUsernameAndPassword(props.getProperty(PropNames.LT_USERNAME), props.getProperty(PropNames.LT_PASSWORD));
        
        nlcService = new NaturalLanguageClassifier();
        nlcService.setUsernameAndPassword(props.getProperty(PropNames.NLC_USERNAME), props.getProperty(PropNames.NLC_PASSWORD));
        classifierId = props.getProperty(PropNames.NLC_INSTANCE);
        
        ttsVoices = ttsService.getVoices();
        sttModels = sttService.getModels();
        ltModels = ltService.getModels();
        
        inputLanguage = "en";
        outputLanguage = "en";
        inputSpeechModel = null;
        inputTranslationModel = null;
        outputTranslationModel = null;
        
        if (props.getProperty(PropNames.TTS_VOICE) != null) {
            setOutputVoice(props.getProperty(PropNames.TTS_VOICE));
        }
        repl();
    }
    
    private void outputDebug(Object o) {
        if (debug) {
            System.out.println(o);
        }
    }
    
    private void output(Object o) {
        System.out.println(o);
    }
    
    private void repl() {
        String line;
        while (true) {
            output("");
            output("Type your query or enter a blank line to use speech to text");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            try {
                System.out.print(inputLanguage + "> ");
                line = br.readLine();
                if (line.equalsIgnoreCase("\\q")) {
                    break;
                } else if (line.startsWith("\\il ")) {
                    setInputLanguage(line.split(" ", 2)[1]);
                } else if (line.startsWith("\\ol ")) {
                    setOutputLanguage(line.split(" ", 2)[1]);
                } else if (line.startsWith("\\ov ")) {
                    setOutputVoice(line.split(" ", 2)[1]);
                } else if (line.trim().equals("\\d")) {
                    debug = !debug;
                    output("debug is now set to: " + debug.toString());
                } else {
                    if (line.equals("")) {
                        line = recordStt();
                        line = translateInput(line);
                        output("I think you said: " + line);
                        playTts("I think you said: " + line);
                    } else {
                        if (!outputLanguage.equals("en")) {
                            line = translateInput(line);
                            output(line);
                        }
                    }
                    Classification c = classifyText(line);
                    outputClassification(c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void outputClassification(Classification c) {
        double topConf = 0.0;
        String topClass = null;
        double twoConf = 0.0;
        String twoClass = null;
        String outputMessage;
        
        // do a simple algorithm to get some idea of confidence
        for (ClassifiedClass cc : c.getClasses()) {
            if (cc.getConfidence() > topConf) {
                twoConf = topConf;
                twoClass = topClass;
                topConf = cc.getConfidence();
                topClass = cc.getName();
            } else if (cc.getConfidence() > twoConf) {
                twoConf = cc.getConfidence();
                twoClass = cc.getName();
            }
        }
        
        if (topConf > 0.90) {
            outputMessage = "I think that's about " + topClass;
        } else if (topConf > 0.70) {
            outputMessage = "I'm pretty sure that's about " + topClass;
        } else if (topConf > 1.15 * twoConf) {
            outputMessage = "That's probably about " + topClass;
        } else {
            outputMessage = "That might be about " + topClass + ". But it could be about " + twoClass + ".";
        }
        output(outputMessage);
        playTts(outputMessage);
    }
    
    private Classification classifyText(String text) {
        Classification c = nlcService.classify(classifierId, text);
        outputDebug(c);
        return c;
    }
    
    private void showLanguages() {
        HashMap<String, Boolean> langs = new HashMap<String, Boolean>();
        for (TranslationModel t : ltModels) {
            if (t.getTarget().equals("en") &&
                    t.getDomain().equals("conversational") &&
                    t.getStatus().equals("available")) {
                langs.put(t.getSource(), new Boolean(false));
            }
        }
        for (SpeechModel s : sttModels) {
            String baseLang = s.getName().split("-")[0];
            if (s.getName().endsWith("BroadbandModel") &&
                    langs.containsKey(baseLang)) {
                langs.put(baseLang, new Boolean(true));
            }
        }
        
        for(Entry<String,Boolean> e : langs.entrySet()) {
            if (e.getValue() == true) {
                output(e.getKey() + " (with speech to text)");
            } else {
                output(e.getKey());
            }
        }
    }

    private String setInputLanguage(String language) {
        SpeechModel newSpeechModel = null;
        TranslationModel newTranslationModel = null;
        
        language = language.trim();

        if (language.equals("en")) {
            inputSpeechModel = null;
            inputTranslationModel = null;
            inputLanguage = "en";
            return "en";
        }

        if (language.equals("?")) {
            output("Available languages");
            showLanguages();
            return null;
        }
        
        for (SpeechModel s : sttModels) {
            if (s.getName().startsWith(language) && 
                    s.getName().endsWith("BroadbandModel")) {
                newSpeechModel = s;
            }
        }
        
        for (TranslationModel t : ltModels) {
            if (t.getSource().equals(language) &&
                    t.getTarget().equals("en") &&
                    t.getDomain().equals("conversational") &&
                    t.getStatus().equals("available")) {
                newTranslationModel = t;
            }
        }
        
        if (newTranslationModel != null) {
            inputSpeechModel = newSpeechModel;
            inputTranslationModel = newTranslationModel;
            output("input language changed to: " + language);
            if (inputSpeechModel == null) {
                output("unfortunately, I'm still learning how to hear that language - so things might be wonky");
            }
            inputLanguage = language;
            return language;
        } else {
            output("I don't have models for the language: " + language);
            output("Reverting to English");
            setInputLanguage("en");
        }
        return null;
    }
    
    private String setOutputLanguage(String language) {
        TranslationModel newTranslationModel = null;
        
        language = language.trim();

        if (language.equals("en")) {
            outputTranslationModel = null;
            return "en";
        }

        for (TranslationModel t : ltModels) {
            if (t.getSource().equals("en") &&
                    t.getTarget().equals(language) &&
                    t.getDomain().equals("conversational")) {
                newTranslationModel = t;
            }
        }
        
        if (newTranslationModel != null) {
            inputTranslationModel = newTranslationModel;
            output("output language changed to: " + language);
            return language;
        }
        return null;
    }
    
    private Voice setOutputVoice(String voice) {
        Voice newOutputVoice = null;
        for (Voice v : ttsVoices) {
            if (v.getName().equals(voice.trim())) {
                newOutputVoice = v;
            }
        }
        
        if (newOutputVoice == null) {
            if (voice.trim().equals("?")) {
                output("usage: \\ov VOICENAME");
            } else {
                output("unable to find voice: " + voice.trim());
            }
            output("available voices:");
            for (Voice v : ttsVoices) {
                output(v.getName());
            }
        } else {
            outputVoice = newOutputVoice;
        }
        return newOutputVoice;
    }
    
    private String recordStt() {
        File outFile;
        try {
            outFile = File.createTempFile("watson", ".wav");
        } catch (IOException e1) {
            e1.printStackTrace();
            return "";
        }
        SoundRecorder sr = new SoundRecorder(outFile.getAbsolutePath());
        sr.start();
        output("Hit enter to stop recording");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
        sr.finish();
        
        output("Running speech to text analysis");
        SpeechResults transcript;
        if (inputSpeechModel == null) {
            transcript = sttService.recognize(outFile, HttpMediaType.AUDIO_WAV);
        } else {
            RecognizeOptions opts = new RecognizeOptions();
            opts.model(inputSpeechModel.getName());
            transcript = sttService.recognize(outFile, HttpMediaType.AUDIO_WAV, opts);
        }
        
        outFile.delete();
        
        outputDebug(transcript);
        return transcript.getResults().get(0).getAlternatives().get(0).getTranscript();
    }

    private String translateInput(String s) {
        if (inputTranslationModel == null) {
            return s;
        }
        
        TranslationResult tr = ltService.translate(s, inputTranslationModel.getModelId());
        outputDebug(tr);
        return tr.getTranslations().get(0).getTranslation();
    }
    
    private void playTts(String s) {
        try {
            InputStream synth;
            if (outputVoice != null) {
                synth = ttsService.synthesize(s, outputVoice, HttpMediaType.AUDIO_WAV);
            } else {
                synth = ttsService.synthesize(s, HttpMediaType.AUDIO_WAV);
            }
            playSound(synth);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void playSound(InputStream istream) throws IOException {
        String line;
        Process tr = Runtime.getRuntime().exec(new String[]{ "/usr/local/bin/play", "-" } );
        BufferedReader rd = new BufferedReader(new InputStreamReader( tr.getInputStream() ) );
        tr.getOutputStream().write(IOUtils.toByteArray(istream));
        tr.getOutputStream().flush();
        tr.getOutputStream().close();
        while((line = rd.readLine()) != null) {
            System.out.println(line);
        }
    }
}
