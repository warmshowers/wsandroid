/***
 * Thanks to Commmonsware, https://github.com/commonsguy/cw-omnibus/blob/master/MapsV2/Animator/src/com/commonsware/android/mapsv2/animator/LegalNoticesActivity.java
  Copyright (c) 2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package fi.bitrite.android.ws.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.widget.TextView;
import com.google.android.gms.common.GooglePlayServicesUtil;
import fi.bitrite.android.ws.R;

public class LegalNoticesActivity extends WSBaseActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.legal);
    initView();


    TextView legal=(TextView)findViewById(R.id.legal);

    String licenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
    if (licenseInfo != null) {
      // licenseInfo is a bit of a mess (coming directly from google)
      // Change the multi-\n to <br/>, then change single \n perhaps followed by whitespace to a space
      // then change the <br/> back to \n
      licenseInfo = licenseInfo.replaceAll("\n\n+", "<br/>").replaceAll("\n[ \t]*", " ").replace("<br/>", "\n");
      legal.setText(licenseInfo);
      Linkify.addLinks(legal, Linkify.ALL);
    }
  }
}
