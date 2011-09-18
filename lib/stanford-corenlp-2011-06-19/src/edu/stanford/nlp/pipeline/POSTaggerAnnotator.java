package edu.stanford.nlp.pipeline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.pipeline.DeprecatedAnnotations.WordsPLAnnotation;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Timing;

/**
 * Wrapper for the maxent part of speech tagger.
 * 
 * @author Anna Rafferty
 * 
 */
public class POSTaggerAnnotator implements Annotator {

  final MaxentTagger pos; // = null;

  private final boolean VERBOSE; // = false;

  private int maxSentenceLength;

  public POSTaggerAnnotator() throws Exception {
    this(true);
  }

  public POSTaggerAnnotator(boolean verbose) throws Exception {
    this(System.getProperty("pos.model", MaxentTagger.DEFAULT_NLP_GROUP_MODEL_PATH), verbose);
  }

  public POSTaggerAnnotator(String posLoc, boolean verbose) throws Exception {
    this(posLoc, verbose, Integer.MAX_VALUE);
  }

  public POSTaggerAnnotator(String posLoc, boolean verbose, int maxSentenceLength) throws Exception {
    this(loadModel(posLoc, verbose), verbose, maxSentenceLength);
  }

  public POSTaggerAnnotator(MaxentTagger model, boolean verbose) {
    this(model, verbose, Integer.MAX_VALUE);
  }

  public POSTaggerAnnotator(MaxentTagger model, boolean verbose,
                            int maxSentenceLength) {
    this.pos = model;
    this.VERBOSE = verbose;
    this.maxSentenceLength = maxSentenceLength;
  }

  public void setMaxSentenceLength(int maxLen) {
    this.maxSentenceLength = maxLen;
  }

  private static MaxentTagger loadModel(String loc, boolean verbose) 
    throws Exception
  {
    Timing timer = null;
    if (verbose) {
      timer = new Timing();
      timer.doing("Loading POS Model [" + loc + ']');
    }
    MaxentTagger tagger = new MaxentTagger(loc);
    if (verbose) {
      timer.done();
    }
    return tagger;
  }

  public void annotate(Annotation annotation) {
    // turn the annotation into a sentence
    if (annotation.has(WordsPLAnnotation.class)) {
      List<List<? extends CoreLabel>> sentences = annotation.get(WordsPLAnnotation.class);
      for (List<? extends CoreLabel> words : sentences) {
        processText(words);
      }
    } else if (annotation.has(CoreAnnotations.SentencesAnnotation.class)) {
      for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
        List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
        ArrayList<TaggedWord> tagged = null;

        tagged = pos.apply(tokens);

        for (int i = 0; i < tokens.size(); ++i) {
          tokens.get(i).set(PartOfSpeechAnnotation.class, tagged.get(i).tag());
          // System.err.println("#" + i + " " + tokens.get(i).word() + ": " +
          // tagged.get(i).tag());
        }
      }
    } else {
      throw new RuntimeException("unable to find words/tokens in: " + annotation);
    }
  }

  /**
   * Takes in a list of words and POS tags them. Tagging is done in place - the
   * returned CoreLabels are the same ones you passed in, with tags added.
   * 
   * @param text
   *          List of tokens to tag
   * @return Tokens with tags
   */
  public List<? extends CoreLabel> processText(List<? extends CoreLabel> text) {
    // cdm 2009: copying isn't necessary; the POS tagger's apply()
    // method does not change the parameter passed in. But I think you
    // can't have it correctly generic without copying. Sigh.

    // if the text size is more than the max length allowed
    if (text.size() > maxSentenceLength) {
      return processTextLargerThanMaxLen(text);
    }

    ArrayList<TaggedWord> tagged = pos.apply(new ArrayList<CoreLabel>(text));
    // copy in the tags
    Iterator<TaggedWord> taggedIter = tagged.iterator();
    for (CoreLabel word : text) {
      TaggedWord cur = taggedIter.next();
      word.setTag(cur.tag());
    }
    return text;
  }

  /**
   * if the text length is more than specified than the text is divided into
   * (length/MaxLen) sentences and tagged individually
   * 
   * @param text
   * @return
   */
  private List<? extends CoreLabel> processTextLargerThanMaxLen(List<? extends CoreLabel> text) {

    int startIndx = 0;
    int endIndx = (startIndx + maxSentenceLength < text.size() ? startIndx + maxSentenceLength : text.size());
    while (true) {
      System.out.println(startIndx + "\t" + endIndx);
      List<? extends CoreLabel> textToTag = text.subList(startIndx, endIndx);
      ArrayList<TaggedWord> tagged = pos.apply(textToTag);

      Iterator<TaggedWord> taggedIter = tagged.iterator();
      for (CoreLabel word : textToTag) {
        TaggedWord cur = taggedIter.next();
        word.setTag(cur.tag());
      }

      if (startIndx + maxSentenceLength >= text.size())
        break;

      startIndx += maxSentenceLength;
      endIndx = (startIndx + maxSentenceLength < text.size() ? startIndx + maxSentenceLength : text.size());

    }
    return text;
  }

}
