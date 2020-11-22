package org.healthnlp.deepphe.util.eval;

public class SiteFeatures {


//      ==================    Site   ==================
//
//>> The following features are neoplasm-independent.
// > The following features involve site class and branch prevalence.
//
//1. Number of exact site class mentions. Normalized over total site class mentions.
//   For instance, forearm (2) vs. abdomen (3) vs. finger (3) ... vs. upper_limb (4) vs. trunk (1).
//   Here the top exact class is upper_limb (4/13).
   private int _siteMentions;
   private int _totalMentions;

//
//2. Number of site class branch mentions.  Normalized over total site class mentions.
//   For instance, [forearm (2), upper_limb (4)]=(6) vs. [abdomen (3), trunk (1)]=(4) vs. [finger (3)]=(3).
//   Here the top class by branch is forearm (6/13).
//   * Another reason to normalize over the total site mentions:  Consider 1 note with 20 site mentions, most populous = (17) vs. 5 notes (or 1 long note) with 100 total site mentions, most populous = (37).  The ratios 17/20 vs. 37/100 are very different from the absolute 17 vs. 37.
//   * Keep in mind that branches can overlap.  For instance, [forearm, upper_limb] (6) vs. [arm, upper_limb] (5) where upper_limb is common with 4 mentions.
   private int _siteBranchSize;
   private int _totalBranchSize;

//
// > The following feature involves class precision.   - A precise class is more likely to be correct than an imprecise class.
//3. Distance of exact site class from root.  Normalize over the furthest distance class.  For instance forearm {3} vs. abdomen {3} vs. finger {3}.
//   * Keep in mind that because of speed constraints we are not walking the ontology through PART_OF (etc.) relations.  (finger P_O hand P_O upper_limb).
   private int _siteNodeDepth;
   private int _deepestNodeDepth;

//
//>> The following are neoplasm-dependent.  They account for relations.
//
//4. Number of "HAS_SITE" relations between site and neoplasm.  Normalized over the total "HAS_SITE" relation count for the neoplasm.
//   Consider that multiple neoplasm mentions can have the same site mention.  "Melanoma on the finger.  The carcinoma is invasive."
//   For instance, consider the neoplasm lung_carcinoma with branch mentions [melanoma (2), carcinoma (3), cancer (1)].  The relation counts for this neoplasm for each site class branch could be:
//   forearm <2>, abdomen <0>, finger <5>.  So, the top score would be finger <5/7>
   private int _siteRelated;
   private int _totalRelated;

//
//*  We now have most mentioned class upper_limb (4/13), most mentioned branch class forearm (6/13), most 'precise' class tied at {3} and most related class branch finger <5>.
//*  Notice that finger <5> relations outnumber branch mentions of finger (3).
//*  The site of the neoplasm is (largely) determined by #5: the number of times neoplasm branch mentions are related to site branch mentions.  This takes into account the different "HAS_SITE" type relations.
//
//5. Next nearest site related to neoplasm, normalized by winning site.  In this case forearm <2/5>.  We could normalize to the total relations <2/7>, but consider relations <3>,<2>,<2>.  I think that the score <2/3> is better than <2/7>.
   private int _secondRelated;

//
//=>  So, our site would be 'finger' and our features 3/13, 3/13, 3/3, 5/7, 2/5.
//
//
//   ==================    SubSite   ==================
//
//Subsite is quite frequently absent.
//Because subsite classes are not normally mentioned with other classes in their same branch, we could skip Site #1 and just use Site #2.
//Likewise, class precision (Site #3) can be skipped.
//Both next-nearest and exact are generally low, so Site #5 can probably be skipped.
//1.  Site #2
//2.  Site #4

}
