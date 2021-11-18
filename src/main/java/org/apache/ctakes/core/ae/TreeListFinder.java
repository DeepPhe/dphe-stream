package org.apache.ctakes.core.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexUtil;
import org.apache.ctakes.core.util.regex.TimeoutMatcher;
import org.apache.ctakes.typesystem.type.textspan.*;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
final public class TreeListFinder extends JCasAnnotator_ImplBase {
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
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jcas ) throws AnalysisEngineProcessException {
      if ( _treeListTypes.isEmpty() ) {
         LOGGER.warn( "No TreeList types defined." );
         return;
      }
      final List<TreeList> treeLists = new ArrayList<>();
      final List<Pair<Integer>> usedTopicSpans = new ArrayList<>();
      final Collection<Topic> topics = JCasUtil.select( jcas, Topic.class );
      if ( topics != null && !topics.isEmpty() ) {
         LOGGER.info( "Annotating TreeLists in Topics ..." );
         for ( Topic topic : topics ) {
            int begin = topic.getBegin();
            String topicText = topic.getCoveredText();
            findTreeLists( jcas, treeLists, begin, topicText );
            final NormalizableAnnotation subject = topic.getSubject();
            if ( subject != null ) {
               usedTopicSpans.add( new Pair<>( Math.min( subject.getBegin(), topic.getBegin() ),
                                               Math.max( subject.getEnd(), topic.getEnd() ) ) );
            } else {
               usedTopicSpans.add( new Pair<>( topic.getBegin(), topic.getEnd() ) );
            }
         }
         usedTopicSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
      }
      final Collection<Paragraph> paragraphs = JCasUtil.select( jcas, Paragraph.class );
      if ( paragraphs != null && !paragraphs.isEmpty() ) {
         LOGGER.info( "Annotating TreeLists in Paragraphs ..." );
         for ( Paragraph paragraph : paragraphs ) {
            if ( paragraphCovered( usedTopicSpans, paragraph ) ) {
               continue;
            }
            findTreeLists( jcas, treeLists, paragraph.getBegin(), paragraph.getCoveredText() );
         }
      } else {
         LOGGER.info( "Annotating TreeLists in Sections ..." );
         for ( Segment section : JCasUtil.select( jcas, Segment.class ) ) {
            final Collection<Pair<Integer>> availableSpans = getAvailableSectionSpans( section, usedTopicSpans );
            for ( Pair<Integer> span : availableSpans ) {
               findTreeLists( jcas,
                              treeLists,
                              span.getValue1(),
                              jcas.getDocumentText()
                                  .substring( span.getValue1(), span.getValue2() ) );
            }
         }
      }
   }

   static private boolean paragraphCovered( final List<Pair<Integer>> usedTopicSpans, final Paragraph paragraph ) {
      if ( usedTopicSpans.isEmpty() ) {
         return false;
      }
      final int begin = paragraph.getBegin();
      final int end = paragraph.getEnd();
      for ( Pair<Integer> span : usedTopicSpans ) {
         if ( span.getValue1() <= begin && end <= span.getValue2() ) {
            return true;
         }
      }
      return false;
   }


   static private Collection<Pair<Integer>> getAvailableSpans( final int windowOffset,
                                                               final int windowLength,
                                                               final Collection<TreeList> treeLists ) {
      final int begin = 0;
      if ( treeLists.isEmpty() ) {
         return Collections.singletonList( new Pair<>( begin, windowLength ) );
      }
      final List<Pair<Integer>> usedTreeListSpans
            = treeLists.stream()
            .filter( t -> t.getEnd() > windowOffset + begin && t.getBegin() < windowOffset + windowLength )
                       .sorted( Comparator.comparingInt( TreeList::getBegin )
                                          .thenComparing( TreeList::getEnd ) )
                       .map( t -> new Pair<>( t.getBegin()-windowOffset, t.getEnd()-windowOffset ) )
                       .collect( Collectors.toList() );
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = begin;
      for ( final Pair<Integer> span : usedTreeListSpans ) {
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < windowLength ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, windowLength ) );
      }
      LOGGER.info( "Spans still available: " + availableSpans.size() + " "
                   + availableSpans.stream()
                                   .map( p -> p.getValue1() +"," + p.getValue2() )
                                   .collect( Collectors.joining( "  " ) ) );
      return availableSpans;
   }


   static private Collection<Pair<Integer>> getAvailableSectionSpans( final Segment section,
                                                                      final List<Pair<Integer>> usedTopicSpans ) {
      if ( usedTopicSpans.isEmpty() ) {
         return Collections.singletonList( new Pair<>( section.getBegin(), section.getEnd() ) );
      }
      final Collection<Pair<Integer>> innerTopics
            = usedTopicSpans.stream()
                            .filter( t -> t.getValue1() >= section.getBegin() && t.getValue2() <= section.getEnd() )
                            .collect( Collectors.toList() );
      if ( innerTopics.isEmpty() ) {
         return Collections.singletonList( new Pair<>( section.getBegin(), section.getEnd() ) );
      }
      final Collection<Pair<Integer>> availableSpans = new ArrayList<>();
      int previousEnd = section.getBegin();
      for ( final Pair<Integer> span : innerTopics ) {
         if ( previousEnd < span.getValue1() ) {
            // previous end is more than one line previous to the new span's begin.  Add span between as available.
            availableSpans.add( new Pair<>( previousEnd, span.getValue1() ) );
         }
         previousEnd = span.getValue2();
      }
      if ( previousEnd < section.getEnd() ) {
         // previous end is more than one line previous to the new span's begin.  Add span between as available.
         availableSpans.add( new Pair<>( previousEnd, section.getEnd() ) );
      }
      return availableSpans;
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
            = new HashSet<>( getAvailableSpans( windowOffset, windowLength, treeLists ) );
      for ( TreeListType treeListType : _treeListTypes ) {
         final List<TreeList> newLists
               = findTreeLists( jCas, windowOffset, windowSpans, windowText, treeListType );
         if ( newLists.isEmpty() ) {
            continue;
         }
         treeLists.addAll( newLists );
         windowSpans.clear();
         windowSpans.addAll( getAvailableSpans( windowOffset, windowLength, treeLists ) );
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
      LOGGER.info( "Finding TreeList " + treeListType.__name );
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
            LOGGER.info( "Matched " + (begin+matcher.start()) + "," + (begin+matcher.end()) );
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
      treeList.setHeading( createHeading( jCas, spanOffset, matcher ) );
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
//      LOGGER.info( "Created TreeList: " + treeList.getTreeListType() + " , " + treeList.getHeading() + "\n"
//                   + treeList.getCoveredText() );
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
      final NormalizableAnnotation header = new NormalizableAnnotation( jCas,
                                            spanOffset + matcher.start(),
                                            spanOffset + matcher.end() );
      final String details = RegexUtil.getGroupText( matcher, REFINEMENT_GROUP );
      if ( !details.isEmpty() ) {
         header.setDetails( details );
      }
      header.addToIndexes();
      return header;
   }


}
