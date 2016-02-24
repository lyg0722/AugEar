package cnsl.augear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
//import edu.cmu.sphinx.frontend.FrontEnd;
//import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
//import edu.cmu.sphinx.tools.feature.FeatureFileDumper;
//import edu.cmu.sphinx.util.props.ConfigurationManager;
//import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Created by leeyg on 2016-02-02.
 */
public class FeatureExtractor {
//    private static final String propertyName = "mfcFrontEnd";
//
//    private ConfigurationManager confManager = null;
//    private PropertySheet pSheet = null;
//    private FrontEnd frontEnd = null;
//    private AudioFileDataSource dataSource = null;
//    private FeatureFileDumper dumper = null;
//
//    public FeatureExtractor(){
//        confManager = new ConfigurationManager("C:\\Users\\leeyg\\Documents\\GitHub\\AugEar\\app\\src\\main\\res\\xml\\mfcc_config.xml");
//        frontEnd = confManager.lookup(propertyName);
//        dataSource = ConfigurationManager.getInstance(AudioFileDataSource.class);           // cmu sample
////        dataSource = new AudioFileDataSource();                                               // my code
//
//        try {
//            dumper = new FeatureFileDumper(confManager, frontEnd.getName());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Feature file starts with total count of numbers of mfcc.
//     * The file contains 39-dimension mfcc of given frames. -> 39x numbers
//     * @param fileObj
//     * @param streamName
//     * @return
//     */
//    public int makeFeatureFile(File fileObj, String streamName){
//        dataSource.setAudioFile(fileObj, streamName);
//        frontEnd.setDataSource(dataSource);                  // substitute mic to audio file
//
//        try {
//            dumper.processFile(fileObj.getAbsolutePath());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        try {
//            dumper.dumpAscii(streamName);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
////        frontEnd.getData();                           // output of the front end
//        return -1;
//    }

}
