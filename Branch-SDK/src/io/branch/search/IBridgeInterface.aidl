// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchContent;

interface IBridgeInterface {
  void addToSharableContent (in BranchUniversalObject contentBUO, in String packageName);
  List<BranchSearchContent> searchContent(String keyword);
}
