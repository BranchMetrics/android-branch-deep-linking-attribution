// IBridgeInterface.aidl
package io.branch.bridge;
import io.branch.indexing.BranchUniversalObject;

interface IBridgeInterface {
  void addToSharableContent (in BranchUniversalObject contentBUO);
  List<BranchUniversalObject> searchContent(String keyword);
}
