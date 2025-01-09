package io.branch.delayedinittest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.branch.delayedinittest.ui.theme.DelayedInitTestTheme
import io.branch.referral.Branch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DelayedInitTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DelayedBranchInitUI { initializeBranch() }
                }
            }
        }
    }

    private fun initializeBranch() {
        // Initialize the Branch SDK
        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            if (error != null) {
                Log.e("BranchSDK_Tester", "branch init failed. Caused by -" + error.message)
            } else {
                Log.i("BranchSDK_Tester", "branch init complete!")
                if (branchUniversalObject != null) {
                    Log.i("BranchSDK_Tester", "title " + branchUniversalObject.title)
                    Log.i("BranchSDK_Tester", "CanonicalIdentifier " + branchUniversalObject.canonicalIdentifier)
                    Log.i("BranchSDK_Tester", "metadata " + branchUniversalObject.contentMetadata.convertToJson())
                }
                if (linkProperties != null) {
                    Log.i("BranchSDK_Tester", "Channel " + linkProperties.channel)
                    Log.i("BranchSDK_Tester", "control params " + linkProperties.controlParams)
                }
            }
        }.withData(this.intent.data).init()
    }

//    fun onNewIntent(intent: Intent?) {
//        if (intent != null) {
//            super.onNewIntent(intent)
//        }
//        this.setIntent(intent);
//        if (intent != null && intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra("branch_force_new_session",false)) {
//            Branch.sessionBuilder(this).withCallback { referringParams, error ->
//                if (error != null) {
//                    Log.e("BranchSDK_Tester", error.message)
//                } else if (referringParams != null) {
//                    Log.i("BranchSDK_Tester", referringParams.toString())
//                }
//            }.reInit()
//        }
//    }

}

@Composable
fun DelayedBranchInitUI(onInitClick: () -> Unit) {
    var isInitialized by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        if (isInitialized) {
            Text("Branch SDK Initialized!")
        } else {
            Button(onClick = {
                onInitClick()
                isInitialized = true
            }) {
                Text("Initialize Branch SDK")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DelayedBranchInitPreview() {
    DelayedInitTestTheme {
        DelayedBranchInitUI {}
    }
}