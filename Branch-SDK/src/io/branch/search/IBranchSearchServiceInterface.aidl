// IBridgeInterface.aidl
package io.branch.search;
import io.branch.indexing.BranchUniversalObject;
import io.branch.search.BranchSearchContent;

interface IBranchSearchServiceInterface {
  void addToSharableContent (in BranchUniversalObject contentBUO, in String packageName, String contentUrl);
  List<BranchSearchContent> searchContent(String keyword);
}
