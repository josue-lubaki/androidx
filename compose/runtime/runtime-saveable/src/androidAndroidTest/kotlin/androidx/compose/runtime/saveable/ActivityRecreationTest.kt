/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime.saveable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.test.R
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityRecreationTest {

    @Test
    fun valueStoredViaRememberIsLost() {
        val activityScenario: ActivityScenario<RecreationTest1Activity> =
            ActivityScenario.launch(RecreationTest1Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
            // change the value
            it.array[0] = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
        }
    }

    @Test
    fun valueStoredViaRememberSavedInstanceStateRestored() {
        val activityScenario: ActivityScenario<RecreationTest2Activity> =
            ActivityScenario.launch(RecreationTest2Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            it.array[0] = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array).isEqualTo(intArrayOf(1))
        }
    }

    @Test
    fun valueStoredInTwoViewsBothRestored() {
        val activityScenario: ActivityScenario<RecreationTest3Activity> =
            ActivityScenario.launch(RecreationTest3Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.array1).isEqualTo(intArrayOf(0))
            assertThat(it.array2).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            it.array1[0] = 1
            it.array2[0] = 2
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.array1).isEqualTo(intArrayOf(1))
            assertThat(it.array2).isEqualTo(intArrayOf(2))
        }
    }

    @Test
    fun valuesStoredInTwoFragmentsRestored() {
        val activityScenario: ActivityScenario<RecreationTest4Activity> =
            ActivityScenario.launch(RecreationTest4Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            val array1 = it.findFragment(R.id.child1).array
            val array2 = it.findFragment(R.id.child2).array
            assertThat(array1).isEqualTo(intArrayOf(0))
            assertThat(array2).isEqualTo(intArrayOf(0))
            // change the value, so we can assert this change will be restored
            array1[0] = 1
            array2[0] = 2
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            val array1 = it.findFragment(R.id.child1).array
            val array2 = it.findFragment(R.id.child2).array
            assertThat(array1).isEqualTo(intArrayOf(1))
            assertThat(array2).isEqualTo(intArrayOf(2))
        }
    }

    @Test
    fun valueStoredInMutableStateIsRestored() {
        val activityScenario: ActivityScenario<RecreationTest5Activity> =
            ActivityScenario.launch(RecreationTest5Activity::class.java)

        activityScenario.moveToState(Lifecycle.State.RESUMED)

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(0)
            // change the value, so we can assert this change will be restored
            it.state.value = 1
        }

        activityScenario.recreate()

        activityScenario.onActivity {
            assertThat(it.state.value).isEqualTo(1)
        }
    }

    private fun FragmentActivity.findFragment(id: Int) =
        supportFragmentManager.findFragmentById(id) as TestFragment
}

class RecreationTest1Activity : ComponentActivity() {

    lateinit var array: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            array = remember { intArrayOf(0) }
        }
    }
}

class RecreationTest2Activity : ComponentActivity() {

    lateinit var array: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            arrayOf<Any?>()
            array = rememberSaveable { intArrayOf(0) }
        }
    }
}

class RecreationTest3Activity : ComponentActivity() {

    lateinit var array1: IntArray
    lateinit var array2: IntArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup
                .LayoutParams.WRAP_CONTENT,
            1f
        )
        val child1 = ComposeView(this)
        child1.id = R.id.child1
        linear.addView(child1, params)
        val child2 = ComposeView(this)
        child2.id = R.id.child2
        linear.addView(child2, params)
        setContentView(linear)

        child1.setContent {
            arrayOf<Any?>()
            array1 = rememberSaveable(key = "key") { intArrayOf(0) }
        }
        child2.setContent {
            arrayOf<Any?>()
            array2 = rememberSaveable(key = "key") { intArrayOf(0) }
        }
    }
}

class RecreationTest4Activity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup
                .LayoutParams.WRAP_CONTENT,
            1f
        )
        val child1 = FrameLayout(this)
        child1.id = R.id.child1
        linear.addView(child1, params)
        val child2 = FrameLayout(this)
        child2.id = R.id.child2
        linear.addView(child2, params)
        setContentView(linear)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.child1, TestFragment())
                .replace(R.id.child2, TestFragment())
                .commitNow()
        }
    }
}

class RecreationTest5Activity : ComponentActivity() {

    lateinit var state: MutableState<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            state = rememberSaveable { mutableStateOf(0) }
        }
    }
}

class TestFragment : Fragment() {

    lateinit var array: IntArray

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setContent {
            arrayOf<Any?>()
            array = rememberSaveable(key = "key") { intArrayOf(0) }
        }
    }
}
