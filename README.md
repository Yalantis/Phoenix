[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Phoenix-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1386) [![Yalantis](https://github.com/Yalantis/Phoenix/blob/master/badge.png)](https://yalantis.com/?utm_source=github)

# Phoenix Pull-to-Refresh

#### This project aims to provide a simple and customizable pull to refresh implementation. Made in [Yalantis] (https://yalantis.com/?utm_source=github)

Check this [project on Dribbble] (https://dribbble.com/shots/1650317-Pull-to-Refresh-Rentals)  
Check this [project on Behance] (https://www.behance.net/gallery/20411445/Mobile-Animations-Interactions)  

<img src="https://d13yacurqjgara.cloudfront.net/users/125056/screenshots/1650317/realestate-pull_1-2-3.gif" alt="alt text" style="width:200;height:200">

#Usage

*For a working implementation, Have a look at the Sample Project - sample*

1. Include the library as local library project.

    ``` compile 'com.yalantis:phoenix:1.2.3' ```

2. Include the PullToRefreshView widget in your layout.

	```xml
    <com.yalantis.phoenix.PullToRefreshView
        android:id="@+id/pull_to_refresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <ListView
            android:id="@+id/list_view"
            android:divider="@null"
            android:dividerHeight="0dp"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

    </com.yalantis.phoenix.PullToRefreshView>
    ```

3. In your `onCreate` method refer to the View and setup OnRefreshListener.
	```java
    mPullToRefreshView = (PullToRefreshView) findViewById(R.id.pull_to_refresh);
    mPullToRefreshView.setOnRefreshListener(new PullToRefreshView.OnRefreshListener() {
        @Override
        public void onRefresh() {
            mPullToRefreshView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullToRefreshView.setRefreshing(false);
                }
            }, REFRESH_DELAY);
        }
     });
     ```

#Customization

To customize drawables you can change:
   * sun.png - Sun image
   * sky.png - background image
   * buildings.png - foreground image

# Misc
If you need to change progress state:
```java
	mPullToRefreshView.setRefreshing(boolean isRefreshing)
```
#Compatibility
  
  * Android GINGERBREAD 2.3+
  
# Changelog

### Version: 1.2

  * Sample updated with RecyclerView example
  * Showing the refresh view just in it's bounds. (Issue with transparent / empty ListView)
  * Possibility to set refresh view padding

### Version: 1.0

  * Initial Build

#### Let us know!

We’d be really happy if you sent us links to your projects where you use our component. Just send an email to github@yalantis.com And do let us know if you have any questions or suggestion regarding the animation. 

P.S. We’re going to publish more awesomeness wrapped in code and a tutorial on how to make UI for Android (iOS) better than better. Stay tuned!

## License

    Copyright 2017, Yalantis

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
