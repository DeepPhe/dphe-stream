package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.ctakes.core.util.StringUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public enum TopoMorphValidator {
INSTANCE;

   static public TopoMorphValidator getInstance() {
      return INSTANCE;
   }

   // Ex: C000, LIP
   private final Map<String,String> _siteClasses = new HashMap<>();
   private final Map<String,String> _siteCodes = new HashMap<>();

   // Ex: C000, [8000/3,8001/1,8002/3]
   private final Map<String, Collection<String>> _topoMorphs = new HashMap<>();

   // Ex: 800, Neoplasm
//   private final Map<String,String> _histoClasses = new HashMap<>();
   private final Map<String,String> _histoCodes = new HashMap<>();

   // Ex: 8000/3, "Neoplasm, Malignant"
//   private final Map<String,String> _morphClasses = new HashMap<>();
   private final Map<String,String> _morphCodes = new HashMap<>();


   TopoMorphValidator() {
      parseValidationFile();
   }

   public String getSiteClass( final String siteCode ) {
      return _siteClasses.getOrDefault( siteCode, "UNKNOWN" );
   }

   public String getSiteCode( final String siteClass ) {
      return _siteCodes.getOrDefault( siteClass, "" );
   }

   public Collection<String> getValidTopoMorphs( final String topoCode ) {
      return _topoMorphs.getOrDefault( topoCode.replace( ".", "" ), Collections.emptyList() );
   }

   public String getHistoCode( final String histologyClass ) {
      return _histoCodes.getOrDefault( histologyClass, "" );
   }

   public String getMorphCode( final String morphologyClass ) {
      return _morphCodes.getOrDefault( morphologyClass, "" );
   }

   
   private void parseValidationFile() {
      File file = new File( "" );
      try {
         file = FileLocator.getFile( "org/healthnlp/deepphe/icdo/DpheHistologySites.bsv" );
      } catch ( FileNotFoundException fnfE ) {
         System.err.println( fnfE.getMessage() );
         System.exit( -1 );
      }
      try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
         int i = 0;
         String line = reader.readLine();
         while ( line != null ) {
            i++;
            if ( i == 1 || line.isEmpty() ) {
               line = reader.readLine();
               continue;
            }
            final String[] splits = StringUtil.fastSplit( line, '|' );
            if ( splits.length < 6 ) {
               line = reader.readLine();
               continue;
            }
            final Collection<String> topoCodes = parseTopoCodes( splits[ 0 ].trim() );
            final String siteDescription = splits[ 1 ].trim();
            final String histology = splits[ 2 ].trim();
            final String histoDescription = splits[ 3 ].trim();
            final String morphology = splits[ 4 ].trim();
            final String morphoDescription = splits[ 5 ].trim();

            // Ex: C000, LIP
            topoCodes.stream().map( c -> c.substring( 0,3 ) ).distinct().forEach( c -> _siteClasses.put( c, siteDescription ) );
            topoCodes.stream().map( c -> c.substring( 0,3 ) ).distinct().forEach( c -> _siteCodes.put( siteDescription, c ) );
            // Ex: C000, [8000/3,8001/1,8002/3]
            topoCodes.forEach( c -> _topoMorphs.computeIfAbsent( c, n -> new HashSet<>() ).add( morphology ) );
            // Ex: 800, Neoplasm
//            _histoClasses.put( histology, histoDescription );
            _histoCodes.put( histoDescription, histology );

            // Ex: 8000/3, "Neoplasm, Malignant"
//            _morphClasses.put( morphology, morphoDescription );
            // Ex: "Neoplasm, Malignant", 8000/3
            _morphCodes.put( morphoDescription, morphology );

            line = reader.readLine();
         }
      } catch ( IOException ioE ) {
         System.out.println( "parseValidationFile " + ioE.getMessage() );
      }
   }

   static private Collection<String> parseTopoCodes( final String codeLine ) {
      final String[] commaCodes = StringUtil.fastSplit( codeLine, ',' );
      if ( commaCodes.length > 1 ) {
         return Arrays.stream( commaCodes )
                      .map( TopoMorphValidator::parseTopoCodes )
                      .flatMap( Collection::stream )
                      .collect( Collectors.toSet() );
      }
      final String[] dashCodes = StringUtil.fastSplit( codeLine, '-' );
      if ( dashCodes.length == 1 ) {
         return Collections.singletonList( codeLine.trim() );
      }
      if ( dashCodes.length != 2 ) {
         System.err.println( "Illegal TopoCodes " + codeLine );
         return Collections.emptyList();
      }
      final int low = parseTopoInt( dashCodes[ 0 ] );
      final int high = parseTopoInt( dashCodes[ 1 ] );
      return getTopoRange( low, high );
   }

   static private int parseTopoInt( final String topoCode ) {
      try {
         final String topoInt = topoCode.trim().substring( 1 );
         return Integer.parseInt( topoInt );
      } catch ( NumberFormatException nfE ) {
         System.err.println( "ParseTopoInt " + topoCode + " " + nfE.getMessage() );
         return 0;
      }
   }

   static private Collection<String> getTopoRange( final int low, final int high ) {
      final Collection<String> topoRange = new HashSet<>();
      for ( int i=low; i<=high; i++ ) {
         topoRange.add( String.format( "C%03d", i ) );
      }
      return topoRange;
   }


//   public static void main( final String ... args ) {
//      final String filePath = args[ 0 ];
//      TopoMorphValidator.getInstance().parseValidation( filePath );
//   }


}
