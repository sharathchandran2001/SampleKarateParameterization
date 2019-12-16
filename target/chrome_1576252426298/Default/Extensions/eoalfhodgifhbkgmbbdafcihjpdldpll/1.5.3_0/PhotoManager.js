function PhotoManager(unittest){ // jshint ignore: line
    var stateTag,
    photoList = [],
    photoListIdx,
    maxPageNb = 1,
    page;

    //get flickr photo links from a specified url
     function getFlickrPhotoList(callback, tagName) {
        var url;
        if (extGlobal.constants.mapQuestUI) {
            if (extGlobal.constants.mapQuestUI && tagName) {
                url = extGlobal.constants.bgphotos_mq_json_path.replace("{tag}", tagName);
            } else if (extGlobal.constants.mapQuestUI && stateTag && stateTag.length > 0) {
                url = extGlobal.constants.bgphotos_mq_json_path.replace("{tag}", stateTag);
            } else { //if we could not find a specific tag/state, we use the default one Y_Ext_TopViews
                url = extGlobal.constants.bgphotos_mq_json_path.replace("{tag}", extGlobal.constants.mqTopViews);
            }
        } else {
            url = extGlobal.constants.bgphotos_json_path.replace("{page}", page);
        }

        var result= function(responseText) {
            var response = JSON.parse(responseText);
            var flickrPhotos = response.photos.photo;

            maxPageNb = response.photos.pages || 1; //read total nb of pages from api call

            if (extGlobal.constants.mapQuestUI && flickrPhotos.length <= extGlobal.constants.mqMinPhotoCache && tagName !== extGlobal.constants.mqTopViews) { //MAPQUEST TODO======================
                var statePhotos = flickrPhotos.slice(); //copying photos by value / new reference
                getFlickrPhotoList(function(topViewPhotos) {
                    callback(statePhotos.concat(topViewPhotos));
                }, extGlobal.constants.mqTopViews); //calling Flickr again with top views instead
            } else { //normal case
                callback(flickrPhotos);
            }
        };
        var err = function(errCode) {
            console.log("api call failed :( ");
        };
        extGlobal.browserGap.xhr(url, result, err);
        extGlobal.browserGap.localStorage.setItem("refreshPhotosDate", JSON.stringify(new Date().getTime()));
    }

     function loadNewPage() {
        photoListIdx = 0;
        page >= maxPageNb ? page = 1 : page++;
        extGlobal.browserGap.localStorage.setItem("photoListIdx", photoListIdx); //reset current index
        extGlobal.browserGap.localStorage.setItem("page", page); //increment page number
        extGlobal.browserGap.localStorage.setItem("cacheIdx", 0); //reset cache
         getFlickrPhotoList(function(flickrPhotos) { //download new batch of photos
            photoList = prunePhotos(flickrPhotos);
            extGlobal.browserGap.localStorage.setItem("photoList", JSON.stringify(photoList));
            cacheNextPhotos(photoListIdx);
        });
    }

     function changeBackgroundPhoto() {
        var returnPhoto = photoList[photoListIdx],
            lastRefresh = extGlobal.browserGap.localStorage.getItem("refreshPhotosDate"),
            now = new Date().getTime();
        cacheNextPhotos(photoListIdx);
        photoListIdx++;
        extGlobal.browserGap.localStorage.setItem("photoListIdx", photoListIdx);
        var bgPhoto =  extGlobal.browserGap.localStorage.getItem("bgPhoto");
        if (photoListIdx >= photoList.length || now - lastRefresh > extGlobal.constants.refreshPhotosTimeLimit || !bgPhoto) {
            // we go to the next page when the page is over or when it is time to refresh the photos in cache (2 weeks)
             loadNewPage();
        }
    }

     //check if retrieved photos can be accepted.If yes, put them in the acceptedPhotos list
    function prunePhotos(list) {
        var checkForRepeats = {},
            acceptedPhotos = [],
            photo,
            ratio;
        for (var p = 0; p < list.length; p++) {
            photo = list[p];
            ratio = photo.width_m/photo.height_m;

            if(photo.media !== extGlobal.constants.video_media &&
            ratio > extGlobal.constants.ratio_min &&
            ratio < extGlobal.constants.ratio_max &&
            !checkForRepeats[photo.id] &&
            (photo.url_m || photo.url_l || photo.url_k)) {
                acceptedPhotos.push(photo);
                checkForRepeats[photo.id] = true;
            }
        }
        var counter = acceptedPhotos.length,
            randomStart = Math.floor(Math.random()*counter);
        while (randomStart > 0) { //modify the array to make it start at acceptedPhotos[randomStart] and put all that was behind at the end of the array
            acceptedPhotos.push(acceptedPhotos[0]);
            acceptedPhotos.shift(0);
            randomStart--;
        }
        return acceptedPhotos;
    }


    /**
     * Function cacheNextPhotos
     *
     * This function will cache the next photos
     * at the first call it will start from currentIndex and cache the next next_cache_size photos (example: 10)
     * on subsequent calls, it will start from cacheIdx and will load photos until there are next_cache_size photos loaded
     * example: first call loads the next 10 photos.
     * At the next newtab, there will only be 9 photos in cache, so it will load 1 more only, and every next new tab will load 1 more
     * so there will always be 10 next photos in cache
     */
     function cacheNextPhotos(currentIndex) {
        var cacheIdx =  extGlobal.browserGap.localStorage.getItem("cacheIdx");
        cacheIdx = parseInt(cacheIdx);
        var cacheSuccess = function(data) {
            var photoInfo = {
                data: data,
                url: this.url,
                id: this.id,
                owner: this.photo.owner,
                ownername: this.photo.ownername
            };
            extGlobal.browserGap.localStorage.setItem("bgPhoto", JSON.stringify(photoInfo));
        };
        var currentCache = cacheIdx || currentIndex;
        for(cacheIdx = currentCache; cacheIdx < parseInt(currentCache) + 1 && cacheIdx < photoList.length-1; cacheIdx++) { //cache next next photo (the one after the following cached one)
            var photo = photoList[cacheIdx];
            var url;
            if (extGlobal.constants.mapQuestUI) {
                url = extGlobal.constants.bgphotos_mq_photos_path + (photo.url_k || photo.url_l || photo.url_m);
            } else {
                url = extGlobal.constants.bgphotos_photos_path + (photo.url_k || photo.url_l || photo.url_m);
            }
            xhrArrayBuffer(url, cacheSuccess.bind({photo: photo, url: url, id: photo.id}));
        }
        extGlobal.browserGap.localStorage.setItem("cacheIdx", cacheIdx);
    }


    /**
     * Function xhrAraryBuffer
     * This function is doing an XHR call to the image
     * and returns the image data
     */

    function xhrArrayBuffer(url, callback){
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.open("GET", url, true);
        xmlhttp.responseType = 'arraybuffer';
        xmlhttp.onload = function(e) {
            var arr = new Uint8Array(this.response);
            var raw = '';
            var i,j,subArray,chunk = 5000;
            for (i=0,j=arr.length; i<j; i+=chunk) {
               subArray = arr.subarray(i,i+chunk);
               raw += String.fromCharCode.apply(null, subArray);
            }
            var b64=btoa(raw);
            callback("data:image/jpeg;base64,"+b64);
        };
        xmlhttp.send();
    }


     function init() {
        // initialize indexes and arrays
        photoListIdx =  extGlobal.browserGap.localStorage.getItem("photoListIdx");
        photoListIdx = parseInt(photoListIdx || 0);
        photoList =  extGlobal.browserGap.localStorage.getItem("photoList");
        photoList ? photoList = JSON.parse(photoList) : photoList = [];
        page =  extGlobal.browserGap.localStorage.getItem("page");
        page = parseInt(page || 1);


        if (!photoList || photoList.length === 0 || !photoListIdx || !page) {
            // reset indexes, get photos, randomize and cache
            photoListIdx = 0;
             extGlobal.browserGap.localStorage.setItem("photoListIdx", 0);
             extGlobal.browserGap.localStorage.setItem("cacheIdx", 0);
             getFlickrPhotoList(function(flickrPhotos) {
                photoList = prunePhotos(flickrPhotos);
                extGlobal.browserGap.localStorage.setItem("photoList", JSON.stringify(photoList));
                cacheNextPhotos(photoListIdx);
            });
        }
        changeBackgroundPhoto();
    }

    /********* MAPQUEST **********/
     function refreshPhotos(filter) {
        var photos,
            mapping = {
                "AL": "Y_Ext_Alabama",
                "AK": "Y_Ext_Alaska",
                "AZ": "Y_Ext_Arizona",
                "AR": "Y_Ext_Arkansas",
                "CA": "Y_Ext_California",
                "CO": "Y_Ext_Colorado",
                "CT": "Y_Ext_Connecticut",
                "DE": "Y_Ext_Delaware",
                "FL": "Y_Ext_Florida",
                "GA": "Y_Ext_Georgia",
                "HI": "Y_Ext_Hawaii",
                "ID": "Y_Ext_Idaho",
                "IL": "Y_Ext_Illinois",
                "IN": "Y_Ext_Indiana",
                "IA": "Y_Ext_Iowa",
                "KS": "Y_Ext_Kansas",
                "KY": "Y_Ext_Kentucky",
                "LA": "Y_Ext_Louisana",
                "ME": "Y_Ext_Maine",
                "MD": "Y_Ext_Maryland",
                "MA": "Y_Ext_Massachusetts",
                "MI": "Y_Ext_Michigan",
                "MN": "Y_Ext_Minnesota",
                "MS": "Y_Ext_Mississippi",
                "MO": "Y_Ext_Missouri",
                "MT": "Y_Ext_Montana",
                "NE": "Y_Ext_Nebraska",
                "NV": "Y_Ext_Nevada",
                "NH": "Y_Ext_New_Hampshire",
                "NJ": "Y_Ext_New_Jersey",
                "NM": "Y_Ext_New_Mexico",
                "NY": "Y_Ext_New_York",
                "NC": "Y_Ext_North_Carolina",
                "ND": "Y_Ext_North_Dakota",
                "OH": "Y_Ext_Ohio",
                "OK": "Y_Ext_Oklahoma",
                "OR": "Y_Ext_Oregon",
                "PA": "Y_Ext_Pennsylvania",
                "RI": "Y_Ext_Rhode_Island",
                "SC": "Y_Ext_South_Carolina",
                "SD": "Y_Ext_South_Dakota",
                "TN": "Y_Ext_Tennessee",
                "TX": "Y_Ext_Texas",
                "UT": "Y_Ext_Utah",
                "VT": "Y_Ext_Vermont",
                "VA": "Y_Ext_Virginia",
                "WA": "Y_Ext_Washington",
                "WV": "Y_Ext_West_Virginia",
                "WI": "Y_Ext_Wisconsin",
                "WY": "Y_Ext_Wyoming",
                "default": extGlobal.constants.mqTopViews
            };

        if (filter.state && mapping[filter.state]) {
            stateTag = mapping[filter.state];
        } else {
            stateTag = mapping.default;
        }

        photoListIdx = 0;
        page = 1;
        extGlobal.browserGap.localStorage.setItem("photoListIdx", photoListIdx); //reset current index
        extGlobal.browserGap.localStorage.setItem("page", page); //page back to 1 (mapquest case)
        extGlobal.browserGap.localStorage.setItem("cacheIdx", 0); //reset cache
         getFlickrPhotoList(function(flickrPhotos) { //download new batch of photos
            photoList = prunePhotos(flickrPhotos);
            extGlobal.browserGap.localStorage.setItem("photoList", JSON.stringify(photoList));
            cacheNextPhotos(photoListIdx);
        });
    }

    function getPhotoInfo(callback) {
        var photoInfo = JSON.parse(extGlobal.browserGap.localStorage.getItem("bgPhoto") || "{}");
        var regexp = /flickr_page_([^0-9]*)_/;
        var tag = (regexp.exec(photoInfo.url).length === 2) ? regexp.exec(photoInfo.url)[1] : null;
        var url = extGlobal.constants.bgphotos_mq_info_json_path.replace("{tag}", tag).replace("{photoId}", photoInfo.id);

        var result = function(responseText) {
            var domParser = new DOMParser();
            var responseXML = domParser.parseFromString(responseText, "text/xml");
            var location = responseXML.querySelector("location");

            if (location) {
                var locality = location.querySelector('locality');
                var county = location.querySelector('county');
                var region = location.querySelector('region');
                var country = location.querySelector('country');

                photoInfo.location = {
                    latitude: location.getAttribute("latitude"),
                    longitude: location.getAttribute("longitude"),
                    locality: locality ? locality.textContent : null,
                    county: county ? county.textContent : null,
                    region: region ? region.textContent : null,
                    country: country ? country.textContent : null
                };
            }
            callback(photoInfo);
        };

        var err = function(errCode) {
            console.log("api call failed :( ");
        };
        if (tag) {
            extGlobal.browserGap.xhr(url, result, err);
        } else {
            callback(null);
        }
    }

    if (unittest) {
        this.getFlickrPhotoList = getFlickrPhotoList;
        this.cacheNextPhotos = cacheNextPhotos;
       
        this.injectFunction = function (){
            getFlickrPhotoList = this.getFlickrPhotoList; // jshint ignore: line
        };
    }

    this.init = init;
    this.changeBackgroundPhoto = changeBackgroundPhoto;
    this.refreshPhotos = refreshPhotos;
    this.getPhotoInfo = getPhotoInfo;

    return this;
}
