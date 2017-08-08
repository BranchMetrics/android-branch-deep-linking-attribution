// IBridgeInterface.aidl
package io.branch.indexing;
import io.branch.indexing.BranchUniversalObject;

interface IBranchListContentInterface {
  void addToIndex (in BranchUniversalObject contentBUO, in String packageName, String contentUrl);
  void deleteFromIndex(in BranchUniversalObject contentBUO, in String packageName);
  void deleteAllFromIndex (in String packageName);
  void addUserInteraction(in BranchUniversalObject contentBUO, in String packageName, String userAction, String contentUrl);
}
