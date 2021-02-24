package org.healthnlp.deepphe.summary.attribute.morphology;

import org.apache.ctakes.core.util.StringUtil;
import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.attribute.SpecificAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;

import java.io.*;
import java.util.*;

import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.addBooleanFeatures;


final public class Morphology implements SpecificAttribute {

   static private final Logger LOGGER = Logger.getLogger( "Morphology" );

   static public String QUOTIENT_TEXT = "";

   private String _bestHistoCode = "";
   private String _bestBehaveCode = "";
   final private NeoplasmAttribute _histology;
   final private NeoplasmAttribute _behavior;

   static public final Map<String,List<String>> CONFUSION = new HashMap<>();
   static public final Map<String,List<String>> REAL_CONFUSION = new HashMap<>();
   static public final Map<String,Integer> SYS_COUNTS = new HashMap<>();
   static public final Map<String,Integer> GOLD_COUNTS = new HashMap<>();
   static private int HIT = 0;
   static private int MISS = 0;

   static private final Map<String,String> GOLD_MAP = parseGold();


   static private Map<String,String> parseGold() {
      final Map<String,String> map = new HashMap<>();
      final String goldPath = "C:\\Spiffy\\data\\dphe_data\\kcr\\CombinedKcrGold.bsv";
      try ( BufferedReader reader = new BufferedReader( new FileReader( goldPath ) ) ){
         String line = reader.readLine();
         while ( line != null ) {
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length > 8 ) {
               map.put( splits[ 0 ], splits[ 7 ] );
            }
            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
      }
      return map;
   }


   public Morphology( final ConceptAggregate neoplasm,
                      final Collection<ConceptAggregate> allConcepts,
                      final Collection<ConceptAggregate> patientNeoplasms,
                      final Collection<String> validTopoMorphs,
                      final String topographyCode ) {
      _histology = createHistoAttribute( neoplasm, allConcepts, patientNeoplasms, validTopoMorphs, topographyCode );
      _behavior = createBehaviorAttribute();
   }


   public String getBestHistoCode() {
      return _bestHistoCode.isEmpty() ? "8000" : _bestHistoCode;
   }

   public String getBestBehaveCode() {
      return _bestBehaveCode.isEmpty() ? "3" : _bestBehaveCode;
   }




   public NeoplasmAttribute toNeoplasmAttribute() {
      return _histology;
   }

   public NeoplasmAttribute getBehaviorAttribute() {
      return _behavior;
   }


   private NeoplasmAttribute createHistoAttribute( final ConceptAggregate neoplasm,
                                                    final Collection<ConceptAggregate> allConcepts,
                                                   final Collection<ConceptAggregate> patientNeoplasms,
                                                   final Collection<String> validTopoMorphs,
                                                   final String topographyCode ) {
      final MorphUriInfoVisitor uriInfoVisitor = new MorphUriInfoVisitor();
      final MorphologyInfoStore patientStore = new MorphologyInfoStore( patientNeoplasms,
                                                                        uriInfoVisitor,
                                                                        validTopoMorphs );

      final MorphologyInfoStore neoplasmStore = new MorphologyInfoStore( neoplasm,
                                                                         uriInfoVisitor,
                                                                         validTopoMorphs );

      _bestHistoCode = neoplasmStore._mainMorphStore._bestHistoCode;
      _bestBehaveCode = neoplasmStore._mainMorphStore._bestBehaviorCode;
//      _morphologyCodes = neoplasmStore._mainMorphStore._sortedMorphCodes;

      final List<Integer> features = createFeatures( neoplasm,
                                                     allConcepts,
                                                     neoplasmStore,
                                                     patientStore );

      final Map<EvidenceLevel, Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( Collections.singletonList( neoplasm ),
                                             neoplasmStore._concepts,
                                             patientStore._concepts );

      final String goldHisto = GOLD_MAP.getOrDefault( neoplasm.getPatientId(),"??" );
      SYS_COUNTS.put( _bestHistoCode, SYS_COUNTS.getOrDefault( _bestHistoCode, 0 )+1 );
      GOLD_COUNTS.put( goldHisto, GOLD_COUNTS.getOrDefault( goldHisto, 0 )+1 );
      if ( _bestHistoCode.equals( goldHisto ) ) {
         HIT++;
      } else {
         MISS++;
         CONFUSION.computeIfAbsent( _bestHistoCode, c -> new ArrayList<>() ).add( goldHisto );
         final List<String> allMorphCodes = neoplasmStore._mainMorphStore._sortedMorphCodes;
         String available = "false";
         if ( allMorphCodes.stream().anyMatch( c -> goldHisto.equals( c.substring( 0,4 ) ) ) ) {
            available = "true";
            REAL_CONFUSION.computeIfAbsent( _bestHistoCode, c -> new ArrayList<>() ).add( goldHisto );
         }
         try ( Writer writer = new FileWriter( "C:/Spiffy/output/morphs.txt", true ) ) {
            writer.write( "\n\n" + neoplasm.getPatientId() + " MORPH : " + _bestHistoCode
                          + " " + topographyCode + " " + neoplasm.getUri()
                          + " " + goldHisto + " " + available + "\n" );
            writer.write( QUOTIENT_TEXT );
            writer.write( MorphCodeInfoStore.URI_ONTO_TEXT );
            writer.write( MorphCodeInfoStore.URI_EXCT_TEXT );
            writer.write( MorphCodeInfoStore.URI_BROD_TEXT );
            writer.write( MorphCodeInfoStore.HIT_COUNT_TEXT );
            writer.write( neoplasm.toText() + "\n");
         } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
   }

   return SpecificAttribute.createAttribute( "histology",
                                                neoplasmStore._mainMorphStore._bestHistoCode,
                                                   evidence,
                                                   features );
   }

   private NeoplasmAttribute createBehaviorAttribute() {
      final List<Integer> behaviorFeatures = new ArrayList<>( _histology.getConfidenceFeatures() );
      behaviorFeatures.addAll( createBehaviorFeatures() );
      return SpecificAttribute.createAttribute( "behavior",
                                                getBestBehaveCode(),
                                                _histology.getDirectEvidence(),
                                                _histology.getIndirectEvidence(),
                                                _histology.getNotEvidence(),
                                                behaviorFeatures );
   }




   private void addMorphStoreFeatures( final List<Integer> features, final MorphologyInfoStore morphStore ) {
      // Neoplasm Uri Strengths
      morphStore.addUriStrengthFeatures( features );
      // Neoplasm Morph Codes.
      morphStore.addMorphFeatures( features );
      morphStore.addMorphStrengthFeatures( features );
   }

   private List<Integer> createFeatures( final ConceptAggregate neoplasm,
                                         final Collection<ConceptAggregate> allConcepts,
                                         final MorphologyInfoStore neoplasmStore,
                                         final MorphologyInfoStore patientStore ) {
      final List<Integer> features = new ArrayList<>();

      neoplasmStore.addGeneralFeatures( features );
      patientStore.addGeneralFeatures( features );
      neoplasmStore.addGeneralRatioFeatures( features, patientStore );

      addMorphStoreFeatures( features, neoplasmStore );
      addMorphStoreFeatures( features, patientStore );

      neoplasmStore.addMorphRatioFeatures( features, patientStore );


      addBooleanFeatures( features,
                          neoplasm.isNegated(),
                          neoplasm.isUncertain(),
                          neoplasm.isGeneric(),
                          neoplasm.isConditional() );

      LOGGER.info( "Features: " + features.size() );
      return features;
   }



   private List<Integer> createBehaviorFeatures() {
      final List<Integer> features = new ArrayList<>( 2 );
      addBooleanFeatures( features, _bestBehaveCode.isEmpty() );
      return features;
   }





}


