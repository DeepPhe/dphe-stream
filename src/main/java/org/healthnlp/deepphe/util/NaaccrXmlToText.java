package org.healthnlp.deepphe.util;

import org.apache.ctakes.core.util.doc.NoteSpecs;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author SPF , chip-nlp
 * @since {3/29/2021}
 */
public class NaaccrXmlToText {

   /**
    * hl7 separates segments with newline \r and/or \r\n.  It uses a long code to represent newlines within fields.
    */
   static private final Pattern CR_PATTERN = Pattern.compile( "\\\\X0D\\\\ *" );
   static private final Pattern LF_PATTERN = Pattern.compile( "\\\\X0A\\\\ *" );
   static private final Pattern TAB_PATTERN = Pattern.compile( "\\\\X09\\\\ *" );
   static private final Pattern ALT_TAB_PATTERN = Pattern.compile( "\\\\F\\\\ *" );
   /**
    * In some places 5 spaces are used to denote newline
    */
   static private final Pattern ALT_LF_PATTERN = Pattern.compile( "(?<!:) {2,}" );


   private enum TextSection {
      // ePath
      PATH_TEXT( "textPathFullText", "PATH", "Full Text", true ),
      SPECIMEN_NATURE( "textPathNatureOfSpecimens", "PATH", "Nature of Specimen", false ),  // when true, scores were the same
      MICRO_DESCRIPTION( "textPathMicroscopicDesc", "PATH", "Microscopic Desc", false ),   // Try true, need to modify relation finders
      FORMAL_DIAGNOSIS( "textPathFormalDx", "PATH", "Final Diagnosis", true ),
      CLINICAL_HISTORY( "textPathClinicalHistory", "PATH", "Clinical History", true ),
      // clinical
      PATH_HISTORY( "textDxProcPe", "DS", "Physical Examination", false ),
      PATH_X_RAY( "textDxProcXRayScan", "DS", "Radiology", false ),
      PATH_MICROSCOPY( "textDxProcScopes", "DS", "Endoscopy", false ),
      PATH_LAB_TESTS( "textDxProcLabTests", "DS", "Labs", false ),
      PATH_OPERATION( "textDxProcOp", "DS", "Operation", false ),
      PATH_DIAGNOSIS( "textDxProcPath", "DS", "Pathology", true ),
      PATH_PRIMARY( "textPrimarySiteTitle", "DS", "Site of Origin", true ),
      PATH_HISTOLOGY( "textHistologyTitle", "DS", "Histology", true ),
      PATH_STAGE( "textStaging", "DS", "Staging", true ),
      PATH_REMARKS( "textRemarks", "DS", "Discussion", false ),
      PATH_PROVIDER( "textPlaceOfDiagnosis", "DS", "Signature", false ),

      UNKNOWN( "UNKNOWN", NoteSpecs.ID_NAME_CLINICAL_NOTE, "Unknown", false );
      private final String _id;
      private final String _noteType;
      private final String _sectionTitle;
      private final boolean _parse;

      TextSection( final String id, final String noteType, final String sectionTitle, final boolean parse ) {
         _id = id;
         _noteType = noteType;
         _sectionTitle = sectionTitle;
         _parse = parse;
      }
      static private TextSection getTextSection( final String tag ) {
         return Arrays.stream( TextSection.values() )
                      .filter( s -> s._id.equalsIgnoreCase( tag ) )
                      .findFirst()
                      .orElse( UNKNOWN );
      }
   }

   static private Collection<File> getFiles( final File xmlDir ) {
      final Collection<File> xmlFiles = new HashSet<>();
      for ( File file : xmlDir.listFiles() ) {
         if ( file.isFile()
              && !file.getName().equals( ".DS_Store" ) ) {
            xmlFiles.add( file );
         } else if ( file.isDirectory()
                     // TODO epath
                     && !file.getName().equals( "naxml" )
                 && !file.getName().startsWith( "emrtext" )
                 && !file.getName().startsWith( "diagrad" ) ) {
               xmlFiles.addAll( getFiles( file ) );
         }
      }
      return xmlFiles;
   }

   static private String readXmlFile( final File xmlFile ) {
      System.out.println( "Parsing " + xmlFile.getPath() );
      final NaaccrXmlHandler handler = new NaaccrXmlHandler();
      try {
         final SAXParserFactory factory = SAXParserFactory.newInstance();
         final SAXParser saxParser = factory.newSAXParser();
         // The following File -> String -> InputStream is done to prevent sax from barfing on special characters.
         // Thank you apple Mac ...
         String xml = "";
         final CharsetDecoder decoder
               = StandardCharsets.UTF_8.newDecoder()
                                       .onMalformedInput( CodingErrorAction.IGNORE );
         try ( BufferedReader reader
                     = new BufferedReader( new InputStreamReader(
                           Files.newInputStream( xmlFile.toPath() ), decoder ) ) ) {
            xml = reader.lines().collect( Collectors.joining( "\n" ) );
         }
         // The first byte of the xml may be a code character and not xml text, which will throw an error in sax.
         xml = xml.trim().replaceFirst( "^([\\W]+)<", "<" );
         xml = normalizeText( xml );
         // sax parse requires an inputstream.
         final InputStream targetStream = new ByteArrayInputStream( xml.getBytes() );
         saxParser.parse( targetStream, handler );
      } catch ( ParserConfigurationException | SAXException | IOException multE ) {
         System.err.println( multE.getMessage() );
         System.exit( 1 );
      }
      return handler.getText();
   }

   /**
    * @param text element text
    * @return text with hl7 newline representations replaced with standard newline characters
    */
   static private String normalizeText( final String text ) {
      if ( text.isEmpty() ) {
         return "";
      }
      final String crFixed = String.join("\r", CR_PATTERN.split(text));
      final String lfFixed = String.join("\n", LF_PATTERN.split(crFixed));
      final String altFixed = String.join("\n", ALT_LF_PATTERN.split(lfFixed));
      final String tabFixed = String.join("\t", TAB_PATTERN.split(altFixed));
      final String tab2Fixed = String.join("\t", ALT_TAB_PATTERN.split(tabFixed));
      final String ampFixed = tab2Fixed.replace( "&amp;amp;", "&amp;" );
      final String lesserFixed = ampFixed.replace( "&amp;lt;", "[" );
      return lesserFixed.replace( "&amp;gt;", "]" );
   }

   static private String getPatientName( final File xmlFile ) {
      String fileName = xmlFile.getName().replace( ".xml", "" );
      final int scoreIndex = fileName.indexOf( '_' );
      if ( scoreIndex < 0 ) {
         return fileName;
      }
      return fileName.substring( 0, scoreIndex );
   }

   static private void writeTextFile( final File outputDir, final File xmlFile, final String text ) {
      final String patientName = getPatientName( xmlFile );
      final File patientDir = new File( outputDir, patientName );
      patientDir.mkdirs();
//      final File outFile = new File( patientDir, patientName + ".txt" );
      final File outFile = new File( outputDir, patientName + ".txt" );
      try ( final Writer writer = new FileWriter( outFile, true ) ) {
         writer.write( text + "\n\n" );
      } catch ( IOException ioE ) {
         System.err.println( ioE.getMessage() );
         System.exit( 1 );
      }
   }

   public static void main( String... args ) {
      final File outputDir = new File( args[ 1 ] );
      System.out.println( args[ 0 ] );
      final Collection<File> xmlFiles = getFiles( new File( args[ 0 ] ) );
      for ( File xmlFile : xmlFiles ) {
         final String text = readXmlFile( xmlFile );
         writeTextFile( outputDir, xmlFile, text );
      }
   }



   static private class NaaccrXmlHandler extends DefaultHandler {
      static private final String ITEM_TAG = "Item";

      private TextSection _currentSection = TextSection.UNKNOWN;
      final private StringBuilder _sb = new StringBuilder();

      private String getText() {
         final String quoteFixed = _sb.toString().replace( "&quot;", "" );
         return quoteFixed.replace( "\"", "" );
//         return _sb.toString();
      }

      /**
       * Receive notification of the start of an element.
       * {@inheritDoc}
       */
      @Override
      public void startElement( final String uri, final String localName, final String tag,
                                final Attributes attributes ) throws SAXException {
         if ( tag.equalsIgnoreCase( ITEM_TAG ) ) {
            final String id = attributes.getValue( "naaccrId" );
            _currentSection = TextSection.getTextSection( id );
//            if ( _currentSection._noteType.equalsIgnoreCase( "DS" ) ) {
//               _sb.append( _currentSection._sectionTitle ).append( "\n" );
//            }
         } else {
            _currentSection = TextSection.UNKNOWN;
         }
      }

      /**
       * Receive notification of the end of an element.
       * {@inheritDoc}
       */
      @Override
      public void endElement( final String uri, final String localName, final String tag ) throws SAXException {
         if ( _currentSection != TextSection.UNKNOWN ) {
            _sb.append( "\n\n" );
         }
         _currentSection = TextSection.UNKNOWN;
      }

      /**
       * Receive notification of character data inside an element.
       * {@inheritDoc}
       */
      @Override
      public void characters( final char[] chars, final int start, final int length ) throws SAXException {
         if ( _currentSection == TextSection.UNKNOWN ) {
            return;
         }
//         final String text = new String( chars, start, length );
         _sb.append( chars, start, length );
      }
   }





}
