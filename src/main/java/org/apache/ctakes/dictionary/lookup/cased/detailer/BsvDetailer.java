package org.apache.ctakes.dictionary.lookup.cased.detailer;


import org.apache.ctakes.dictionary.lookup.cased.util.bsv.BsvFileParser;
import org.apache.ctakes.dictionary.lookup.cased.util.bsv.StringArrayCreator;
import org.apache.ctakes.utils.env.EnvironmentVariable;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;

import java.io.IOException;
import java.util.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 8/18/2020
 */
final public class BsvDetailer implements TermDetailer {

   static public final String DETAILER_TYPE = "BSV";

   static private final Logger LOGGER = Logger.getLogger( "BsvDetailer" );


   private final InMemoryDetailer _delegate;

   public BsvDetailer( final String name, final UimaContext uimaContext ) {
      this( name, EnvironmentVariable.getEnv( name + "_file", uimaContext ) );
   }

   public BsvDetailer( final String name, final String bsvPath ) {
      final Map<String, Details> detailsMap = parseBsvFile( name, bsvPath );
      _delegate = new InMemoryDetailer( name, detailsMap );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public String getName() {
      return _delegate.getName();
   }


   /**
    * {@inheritDoc}
    * @param cui
    * @return
    */
   @Override
   public Details getDetails( final String cui ) {
      return _delegate.getDetails( cui );
   }


   /**
    * Create a map of {@link Details} Objects
    * by parsing a bsv file.  The file should have a columnar format:
    * <p>
    * CUI|Code
    * </p>
    *
    * @param bsvFilePath path to file containing term rows and bsv columns
    * @return map of all cuis and codes read from the bsv file
    */
   static private Map<String, Details> parseBsvFile( final String name, final String bsvFilePath ) {
      final Collection<String[]> columnCollection = new HashSet<>();
      try {
         columnCollection.addAll( BsvFileParser.parseBsvFile( bsvFilePath, new StringArrayCreator( 5 ) ) );
      } catch ( IOException ioE ) {
         LOGGER.error( ioE.getMessage() );
      }
      if ( columnCollection.isEmpty() ) {
         return Collections.emptyMap();
      }
      final Map<String, Details> detailsMap = new HashMap<>();
      for ( String[] columns : columnCollection ) {
         detailsMap.put( columns[0], new Details( columns[1], columns[2], columns[3], getShort( columns[4] ) ) );
      }
      return detailsMap;
   }

   static private short getShort( final String shorty ) {
      if ( shorty.isEmpty() ) {
         return (short)0;
      }
      try {
         return Short.parseShort( shorty );
      } catch ( NumberFormatException nfE ) {
         //
      }
      return (short)0;
   }


}