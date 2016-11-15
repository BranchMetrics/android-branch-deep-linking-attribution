// IBridgeInterface.aidl
package io.branch.bridge;
import io.branch.indexing.BranchUniversalObject;
import io.branch.bridge.BranchSearchContent;

interface IBridgeInterface {
  void addToSharableContent (in BranchUniversalObject contentBUO, in String packageName);
  List<BranchSearchContent> searchContent(String keyword);
}
