package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.core.util.find.WindowFinderUtil;
import org.apache.ctakes.core.util.find.WindowedFinder;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.core.util.regex.TimeoutMatcher;
import org.apache.ctakes.typesystem.type.textspan.FormattedList;
import org.apache.ctakes.typesystem.type.textspan.NormalizableAnnotation;
import org.apache.ctakes.typesystem.type.textspan.TreeList;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SPF , chip-nlp
 * @since {10/12/2021}
 */
@PipeBitInfo(
      name = "Tree List Finder",
      description = "Annotates formatted List Sections by detecting them using Regular Expressions provided in an input File.",
      dependencies = { PipeBitInfo.TypeProduct.SECTION },
      products = { PipeBitInfo.TypeProduct.LIST }
)
final public class TreeListFinder extends JCasAnnotator_ImplBase implements WindowedFinder<TreeList> {
// TODO Replace ctakes ListAnnotator in core.ae with this.

   static private final Logger LOGGER = Logger.getLogger( "TreeListFinder" );


   static public final String LIST_TYPES_PATH = "TreeListRegexBsv";
   static private final String LIST_TYPES_DESC
         = "path to a file containing a list of regular expressions and corresponding list types.";


   @ConfigurationParameter(
         name = LIST_TYPES_PATH,
         description = LIST_TYPES_DESC,
         defaultValue = "org/apache/ctakes/core/treelist/DefaultTreeListRegex.bsv"
   )
   private String _listRegexPath;

// The new format is:
// List Name || Full List Regex || Single List Line Regex
// Where Single Line Regex may contain the named groups <Index> <Name> <Value> <Details>

   static private final String HEADING_GROUP = "Heading";
   static private final String REFINEMENT_GROUP = "Refinement";
   static private final String LIST_GROUP = "List";


   /**
    * Holder for list type as defined in the user's specification bsv file
    */
   static private final class TreeListType {
      private final String __name;
      private final Pattern __fullPattern;
      private final Pattern __entryPattern;

      private TreeListType( final String name,
                        final Pattern fullPattern,
                        final Pattern entryPattern ) {
         __name = name;
         __fullPattern = fullPattern;
         __entryPattern = entryPattern;
      }
   }

   private final List<TreeListType> _treeListTypes = new ArrayList<>();

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
      try {
         final List<RegexUtil.RegexItemInfo> itemInfos
               = RegexUtil.parseBsvFile( _listRegexPath,
                                         2,
                                         "TreeList Name/Type "
                                         + "|| Full Regex; Heading, {Refinement}, List "
                                         + "|| List Entry Regex; Name, Value" );
         for ( RegexUtil.RegexItemInfo itemInfo : itemInfos ) {
            final List<Pattern> patternList = itemInfo.getPatternList();
            final TreeListType treeListType = new TreeListType( itemInfo.getName(),
                                                                patternList.get( 0 ),
                                                                patternList.get( 1 ) );
            _treeListTypes.add( treeListType );
         }
      } catch ( IOException ioE ) {
         throw new ResourceInitializationException( ioE );
      }
      if ( _treeListTypes.isEmpty() ) {
         LOGGER.warn( "No TreeList types defined.  TreeList Finding will be skipped." );
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _treeListTypes.isEmpty() ) {
         return;
      }
      WindowFinderUtil.findInWindows( jcas, LOGGER, "Finding TreeLists", this );
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<TreeList> addFound( final JCas jcas, final Integer windowOffset, final String windowText,
                                   final List<TreeList> foundItems ) {
      return findTreeLists( jcas, foundItems, windowOffset, windowText );
   }


   private List<TreeList> findTreeLists( final JCas jCas,
                                         final List<TreeList> treeLists,
                                         final int windowOffset,
                                         final String windowText ) {
      if ( windowText.trim().length() <= 3 ) {
         return treeLists;
      }
      final int windowLength = windowText.length();
      final Collection<Pair<Integer>> windowSpans
            = new HashSet<>( TextSpanUtil.getAvailableSpans( windowOffset, windowLength, treeLists ) );
      for ( TreeListType treeListType : _treeListTypes ) {
         final List<TreeList> newLists
               = findTreeLists( jCas, windowOffset, windowSpans, windowText, treeListType );
         if ( newLists.isEmpty() ) {
            continue;
         }
         treeLists.addAll( newLists );
         windowSpans.clear();
         windowSpans.addAll( TextSpanUtil.getAvailableSpans( windowOffset, windowLength, treeLists ) );
         if ( windowSpans.isEmpty() ) {
            break;
         }
      }
      return treeLists;
   }

   static private List<TreeList> findTreeLists( final JCas jCas,
                                                final int windowOffset,
                                                final Collection<Pair<Integer>> windowSpans,
                                                final String windowText,
                                                final TreeListType treeListType ) {
      final List<TreeList> treeLists = new ArrayList<>();
      for ( final Pair<Integer> span : windowSpans ) {
         treeLists.addAll(
               findTreeLists( jCas, windowOffset, span.getValue1(), span.getValue2(), windowText, treeListType) );
      }
      return treeLists;
   }


   static private List<TreeList> findTreeLists( final JCas jCas,
                                                final int windowOffset,
                                                final int begin,
                                                final int end,
                                                final String windowText,
                                                final TreeListType treeListType ) {
      final String spanText = windowText.substring( begin, end );
      if ( spanText.trim().length() <= 3 ) {
         return Collections.emptyList();
      }
      final List<TreeList> treeLists = new ArrayList<>();
      try ( TimeoutMatcher finder = new TimeoutMatcher( treeListType.__fullPattern, spanText ) ) {
         Matcher matcher = finder.nextMatch();
         while ( matcher != null ) {
            final TreeList treeList
                  = createTreeList( jCas, windowOffset + begin, spanText, matcher, treeListType );
            treeLists.add( treeList );
            matcher = finder.nextMatch();
         }
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.error( iaE.getMessage() );
      }
      return treeLists;
   }


   static private TreeList createTreeList( final JCas jCas,
                                           final int spanOffset,
                                           final String spanText,
                                           final Matcher matcher,
                                           final TreeListType treeListType ) {
      final TreeList treeList = new TreeList( jCas,
                                              spanOffset + matcher.start(),
                                              spanOffset + matcher.end() );
      treeList.setTreeListType( treeListType.__name );
      final NormalizableAnnotation heading = createHeading( jCas, spanOffset, matcher );
      if ( heading != null ) {
         treeList.setHeading( heading );
      }
      final Pair<Integer> listSpan = RegexUtil.getGroupSpan( matcher, LIST_GROUP );
      if ( RegexUtil.isValidSpan( listSpan ) ) {
         final String listText = spanText.substring( listSpan.getValue1(), listSpan.getValue2() );
         final FormattedList list = ListFinder.createList( jCas,
                                                           spanOffset + listSpan.getValue1(),
                                                           listText,
                                                           treeListType.__name,
                                                           treeListType.__entryPattern );
         treeList.setList( list );
      }
      treeList.addToIndexes();
      return treeList;
   }


   static private NormalizableAnnotation createHeading( final JCas jCas,
                                                       final int spanOffset,
                                                       final Matcher matcher ) {
      final Pair<Integer> span = RegexUtil.getGroupSpan( matcher, HEADING_GROUP );
      if ( !RegexUtil.isValidSpan( span ) ) {
         return null;
      }
//      LOGGER.info( "Creating Normalizable " + HEADING_GROUP + " " + (spanOffset + span.getValue1()) + "-" + (spanOffset + span.getValue2()) );
      final NormalizableAnnotation header = new NormalizableAnnotation( jCas,
                                            spanOffset + span.getValue1(),
                                            spanOffset + span.getValue2() );
      final String details = RegexUtil.getGroupText( matcher, REFINEMENT_GROUP );
      if ( !details.isEmpty() ) {
         header.setDetails( details );
      }
      header.addToIndexes();
      return header;
   }


   static public AnalysisEngineDescription createEngineDescription( final String listTypesPath )
         throws ResourceInitializationException {
      return AnalysisEngineFactory.createEngineDescription( TreeListFinder.class,
                                                            LIST_TYPES_PATH, listTypesPath );
   }


}
