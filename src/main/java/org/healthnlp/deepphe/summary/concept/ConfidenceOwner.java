package org.healthnlp.deepphe.summary.concept;

/**
 * @author SPF , chip-nlp
 * @since {3/19/2023}
 */
public interface ConfidenceOwner {

   default double getConfidence() {
      return 0;
   }

}
