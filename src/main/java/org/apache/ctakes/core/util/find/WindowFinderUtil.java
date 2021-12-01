package org.apache.ctakes.core.util.find;

import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.TextSpanUtil;
import org.apache.ctakes.typesystem.type.textspan.NormalizableAnnotation;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Segment;
import org.apache.ctakes.typesystem.type.textspan.Topic;
import org.apache.log4j.Logger;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @author SPF , chip-nlp
 * @since {12/1/2021}
 */
final public class WindowFinderUtil {

   private WindowFinderUtil() {}

   static public <A extends Annotation> List<A> findInWindows( final JCas jCas,
                                                               final WindowedFinder<A> windowedFinder ) {
      return findInWindows( jCas, null, "", windowedFinder );
   }

   static public <A extends Annotation> List<A> findInWindows( final JCas jCas,
                                                              final Logger logger,
                                                              final String processName,
                                                              final WindowedFinder<A> windowedFinder ) {

      final List<A> foundItems = new ArrayList<>();
      final List<Pair<Integer>> usedTopicSpans = new ArrayList<>();
      final Collection<Topic> topics = org.apache.uima.fit.util.JCasUtil.select( jCas, Topic.class );
      if ( topics != null && !topics.isEmpty() ) {
         if ( logger != null && !processName.isEmpty() ) {
            logger.info( processName + " in Topics ..." );
         }
         for ( Topic topic : topics ) {
            int topicOffset = topic.getBegin();
            windowedFinder.addFound( jCas, topicOffset, topic.getCoveredText(), foundItems );
            final NormalizableAnnotation subject = topic.getSubject();
            if ( subject != null ) {
               usedTopicSpans.add( new Pair<>( Math.min( subject.getBegin(), topicOffset ),
                                               Math.max( subject.getEnd(), topic.getEnd() ) ) );
            } else {
               usedTopicSpans.add( new Pair<>( topicOffset, topic.getEnd() ) );
            }
         }
         usedTopicSpans.sort( Comparator.comparingInt( Pair::getValue1 ) );
      }
      final Collection<Paragraph> paragraphs = org.apache.uima.fit.util.JCasUtil.select( jCas, Paragraph.class );
      if ( paragraphs != null && !paragraphs.isEmpty() ) {
         if ( logger != null && !processName.isEmpty() ) {
            logger.info( processName + " in Paragraphs ..." );
         }
         for ( Paragraph paragraph : paragraphs ) {
            if ( TextSpanUtil.isAnnotationCovered( usedTopicSpans, paragraph ) ) {
               continue;
            }
            windowedFinder.addFound( jCas, paragraph.getBegin(), paragraph.getCoveredText(), foundItems );
         }
      } else {
         if ( logger != null && !processName.isEmpty() ) {
            logger.info( processName + " in Sections ..." );
         }
         for ( Segment section : JCasUtil.select( jCas, Segment.class ) ) {
            final Collection<Pair<Integer>> availableSpans = TextSpanUtil.getAvailableSpans( section, usedTopicSpans );
            for ( Pair<Integer> span : availableSpans ) {
               final String spanText = jCas.getDocumentText()
                                           .substring( span.getValue1(), span.getValue2() );
               windowedFinder.addFound( jCas, span.getValue1(), spanText, foundItems );
            }
         }
      }
      return foundItems;
   }


}
