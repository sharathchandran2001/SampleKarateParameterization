chrome.runtime.onMessage.addListener(preInit);
var extGlobal = {}; //jshint ignore: line
extGlobal.constants = new Constants();
extGlobal.browserGap = new BrowserGap();
extGlobal.photoManager = new PhotoManager();
extGlobal.trendingNow = typeof TrendingNow !== 'undefined' ? new TrendingNow() : null;
extGlobal.tracker = new Tracker();
extGlobal.tabs = chrome.tabs;
extGlobal.photoManager.init();
extGlobal.utils = new ViewUtils();

if (extGlobal.constants.weatherUI) {
    extGlobal.weather = new Weather();
    extGlobal.weather.init();
}
if (extGlobal.constants.sportsUI) {
    extGlobal.sports = new Sports();
    extGlobal.sports.init();
}
if (extGlobal.constants.mapQuestUI) {
    extGlobal.mapQuest = new MapQuest();
    extGlobal.mapQuest.init();
}
extGlobal.browserGap.loadTrackingParams();

if (!extGlobal.constants.aolUI) {
    chrome.runtime.setUninstallURL(extGlobal.constants.extensionUninstallUrl);
}


chrome.runtime.onSuspend.addListener(function() {
    if (extGlobal.constants.financeUI) {
        extGlobal.browserGap.localStorage.removeItem("financeWatchList");
    }
});

chrome.runtime.getPlatformInfo(function (platformInfo) { //setting the platform in extGlobal for further access
    extGlobal.platform = platformInfo ? platformInfo.os : "win"; //windows by default
});
initFirstRun();
fetchDistributionChannel();
extGlobal.trendingNow && extGlobal.trendingNow.init();

if (extGlobal.constants.breakingNewsUI) {
    extGlobal.breakingNews = new BreakingNews();
    extGlobal.breakingNews.init();
}

if (extGlobal.constants.financeUI) {
    extGlobal.finance = new Finance();
    extGlobal.finance.init();
}

if (extGlobal.constants.aolUI) {
    extGlobal.aol = new Aol();
    extGlobal.aol.init();
}

extGlobal.browserGap.addNewTabListener(function(msg, response){
    if(msg.renderNewTab) {
        var newTabData = {};
        if (extGlobal.constants.weatherUI) {
            extGlobal.weather.refreshWeatherDataCache();
        }
        if (extGlobal.constants.sportsUI) {
            newTabData.sportsData = extGlobal.sports.loadSportsData();
            extGlobal.sports.refreshgameIDDataCache(true);
        }
        newTabData.topSites = extGlobal.browserGap.getTopSites();
        chrome.history ? newTabData.history = extGlobal.browserGap.getHistory() : null;
        newTabData.bookmarks = extGlobal.browserGap.getBookmarks();
        newTabData.otherBookmarks = extGlobal.browserGap.getOtherBookmarks();
        newTabData.distributionChannel = extGlobal.distributionChannel;
        newTabData.trendingNowData = extGlobal.browserGap.localStorage.getItem("trendingStories");
        if (extGlobal.constants.breakingNewsUI) {
            newTabData.breakingNews = extGlobal.breakingNews.loadBreakingNews();
        }
        newTabData.enableTN = extGlobal.enableTN; //breaking news takes over TN

        if (extGlobal.constants.financeUI) {
            newTabData.financeData = extGlobal.finance.loadFinanceData(); //shows last quote data from local storage
            extGlobal.finance.refreshFinanceData(true); //will refresh quotes shortly after tab opens ()
        }
        if (extGlobal.constants.aolUI) {
            newTabData.aolData = extGlobal.aol.loadData();
        }
        if (extGlobal.constants.mapQuestUI && extGlobal.geolocation && extGlobal.geolocation.coords) {
            newTabData.mqGeolocation = {
                lat: extGlobal.geolocation.coords.latitude,
                long: extGlobal.geolocation.coords.longitude,
                country: extGlobal.geolocation.country
            };
        }
        if (!extGlobal.browserGap.localStorage.getItem("firstTabCompleted") && extGlobal.platform === "win") {
            var now = new Date().getTime();
            var installTime = extGlobal.browserGap.localStorage.getItem("firstRunCompletedTime");
            if ((installTime && (now - installTime) / 86400000) < 1) {
                //in an update scenario, user doesn't have firstTabCompleted but firstRunCompletedTime is already old.
                //let's say that if firstRunCompletedTime is more than 1 day old, we don't display the first tab interstitial
                newTabData.firstTab = true;
            }
            extGlobal.browserGap.localStorage.setItem("firstTabCompleted", true);
        }
        extGlobal.photoManager.changeBackgroundPhoto();

        response(newTabData);

    }
    if(msg.tracker){
        msg.beaconConfig.params.browser = extGlobal.constants.tracker_browser_chr;
        if(msg.pageInfo) {
            extGlobal.tracker.sendBeacon(extGlobal.constants.distributionChannels[extGlobal.distributionChannel].chrome_space_id || extGlobal.constants.chrome_space_id, extGlobal.constants.tracker_page_info, msg.beaconConfig);
        }
        else if(msg.linkView){
              extGlobal.tracker.sendBeacon(extGlobal.constants.distributionChannels[extGlobal.distributionChannel].chrome_space_id || extGlobal.constants.chrome_space_id, extGlobal.constants.tracker_link_view, msg.beaconConfig);
        }
        else {
            extGlobal.tracker.sendBeacon(extGlobal.constants.distributionChannels[extGlobal.distributionChannel].chrome_space_id || extGlobal.constants.chrome_space_id, extGlobal.constants.tracker_click_info, msg.beaconConfig);
        }
    }

    if (msg.redirectTo && msg.site) {
        extGlobal.tabs.query({active: true, currentWindow: true}, function(tabs) {
            if (tabs && tabs[0] && tabs[0].url === "chrome://newtab/") {
                chrome.tabs.update(tabs[0].id, {url: msg.site});
            }
        });
    }
    if (msg.addQuote && msg.symbol) {
        extGlobal.finance.addQuote(msg.symbol);
    }
    if (msg.deleteQuote && msg.symbol) {
        extGlobal.finance.deleteQuote(msg.symbol);
    }
    if (msg.watchListView) {
        extGlobal.finance.setView(msg.watchListView);
    }
    if (msg.logOut) {
        extGlobal.finance.logOut();
    }
    if (msg.photoInfo) {
        extGlobal.photoManager.getPhotoInfo(function(photoInfo) {
            try {
                response(photoInfo);
            } catch (e) {
                //error can happen when a tab is quickly reloaded before the background script sends data back.
            }
        });
        // return true to keep the message channel opened
        return true;
    }
    if (msg.changeSports) {
        extGlobal.sports.setSport(msg.selection);
        extGlobal.sports.refreshgameIDDataCache(false, true);
    }
}, preInit);


function preInit( msg, sender, response) {
    if(msg.newTab) {
        response(null);
    }
}


chrome.runtime.onInstalled.addListener(function(details) {
    if (details.reason === "install") {
        if (extGlobal.constants.dynamicSearch) {
            var distribution_channel,
                urlPattern = ["https://*.yahoo.com/*", "https://chrome.google.com/webstore/*"];
            extGlobal.tabs.query({"url": urlPattern}, function (tabs) {
                distribution_channel = extGlobal.utils.chromeCheckPartner(tabs) || extGlobal.constants.distributionDefaultChannel;
                extGlobal.distributionChannel = distribution_channel;
                extGlobal.browserGap.localStorage.setItem("partner", distribution_channel);
            });
        } else {
            extGlobal.distributionChannel = extGlobal.constants.distributionDefaultChannel;
            extGlobal.browserGap.localStorage.setItem("partner", extGlobal.distributionChannel);
        }

        if ((extGlobal.constants.distributionChannels[extGlobal.distributionChannel].postInstallPage || "") === "newtab") {
            setTimeout(function () {
                chrome.tabs.create({url: "chrome://newtab"});
            }, 2000);
        }
    }
});

function fetchDistributionChannel() {
    extGlobal.distributionChannel = extGlobal.browserGap.localStorage.getItem("partner") || extGlobal.constants.distributionDefaultChannel;
    console.log(extGlobal.distributionChannel);
}

function isFirstRunCompleted()
{
    return JSON.parse(extGlobal.browserGap.localStorage.getItem("firstRunCompleted"));
}

function initFirstRun(){
    if(!isFirstRunCompleted()) {
        var now = new Date();
        extGlobal.browserGap.localStorage.setItem("firstRunCompleted", JSON.stringify(true));
        extGlobal.browserGap.localStorage.setItem("firstRunCompletedTime", JSON.stringify(now.getTime()));
        if(!extGlobal.browserGap.isOnline()) {
            extGlobal.browserGap.onceOnline(sendInstallPing);
        }else {
            sendInstallPing();
        }
    }
}

function sendInstallPing(){
    var beaconConfig = {};
    var beaconParams = {};
    beaconParams.itype = extGlobal.constants.tracker_install;
    beaconParams.browser = extGlobal.constants.tracker_browser_chr;
    beaconConfig.params = beaconParams;
    if (extGlobal.constants.breakingNewsUI) { //the breaking news
        beaconConfig.params.tn_enable = extGlobal.constants.tn_enable_value;
    }
    setTimeout(function() {
        extGlobal.tracker.sendBeacon(extGlobal.constants.distributionChannels[extGlobal.distributionChannel].chrome_space_id || extGlobal.constants.chrome_space_id, extGlobal.constants.tracker_page_info, beaconConfig);
    }, 1000);
}

setTimeout(function() {
    extGlobal.tracker.initAlivePing(extGlobal.constants.distributionChannels[extGlobal.distributionChannel].chrome_space_id || extGlobal.constants.chrome_space_id, extGlobal.constants.tracker_browser_chr);
}, 1000);


chrome.runtime.onConnect.addListener(function(port) {
    port.postMessage(extGlobal.browserGap.getGDPRprivacyObject());
    /*port.onMessage.addListener(function(msg) {
        // See other examples for sample onMessage handlers.
        console.log('onConnect->onMessage', msg);
    });*/
});

chrome.runtime.onConnectExternal.addListener(function(port) {
    port.postMessage(extGlobal.browserGap.getGDPRprivacyObject());
    /*port.onMessage.addListener(function(msg) {
      // See other examples for sample onMessage handlers.
      console.log('onConnectExternal->onMessage', msg);
    });*/
});


chrome.browserAction.onClicked.addListener(function(activeTab) {
    window.open('newtab.html','_blank');
});



/* ******************************************************************************* */
//                     WebRequest for adding header to Yahoo                       //
/* ******************************************************************************* */
if (chrome.webRequest) {
    chrome.webRequest.onBeforeSendHeaders.addListener(function (details) {
        function isMainFrame(reqDetails) {
            return reqDetails.type === "main_frame" && reqDetails.frameId === 0;
        }
        function addExtensionHeader() {
            var found = false;
            for (var i=0; details.requestHeaders && i < details.requestHeaders.length; i++) {
                if (details.requestHeaders[i].name === extGlobal.constants.headerExtName) {
                    details.requestHeaders[i].value += "," + extGlobal.constants.headerExtValue;
                    found = true;
                    break;
                }
            }
            if (!found) {
                details.requestHeaders.push({"name": extGlobal.constants.headerExtName, "value": extGlobal.constants.headerExtValue});
            }
            return {requestHeaders: details.requestHeaders};
        }
        if (isMainFrame(details)) {
            return addExtensionHeader();
        }
    }, {
        urls: ["https://*.yahoo.com/*"] // List of URLs
    }, ["blocking", "requestHeaders"]); // Block intercepted requests until this handler has finished
}
/* ******************************************************************************* */
//                       WebRequest for Dynamic Search Set                         //
/* ******************************************************************************* */
if (chrome.webRequest) {
    chrome.webRequest.onBeforeRequest.addListener(function (details) {

        function isYahooSearch(url) { //the domain part is already taken care of by the regular expression in the webRequest listener
            var hasSearchPath = url.indexOf("/search") > -1;
            var hasSearchQuery = url.indexOf("p=") > -1;
            return hasSearchPath && hasSearchQuery;
        }

        function isCorrectTrackingParams(url) {
            var partner = extGlobal.distributionChannel,
                partnerConfig = extGlobal.constants.distributionChannels[partner],
                isPathCorrect = partnerConfig.hsimp ? url.indexOf("/yhs/search") > -1 : (url.indexOf("/search") > -1 && url.indexOf("/yhs/search") === -1),
                isParamCorrect,
                isTn = extGlobal.utils.extractParam("fr", url) === extGlobal.constants.tnFrCode;
            if (isTn) { //user clicked on the TN link - no need to reroute
                isParamCorrect = true;
                isPathCorrect = true;
            } else if (partnerConfig.hsimp) {
                isParamCorrect = extGlobal.utils.extractParam("hsimp", url) === partnerConfig.hsimp && extGlobal.utils.extractParam("hspart", url) === partnerConfig.hspart;
                if (partnerConfig.subCampaigns) {
                    var campaign = extGlobal.browserGap.localStorage.getItem("campaign");
                    if (campaign && campaign.indexOf(extGlobal.constants.installDateTag) > -1) {
                        var installTimestamp = extGlobal.browserGap.localStorage.getItem("firstRunCompletedTime");
                        campaign = campaign.replace(extGlobal.constants.installDateTag, extGlobal.utils.getInstallDateTag(installTimestamp));
                    }
                    var cmpgParam = partnerConfig.cmpgParam;
                    if (cmpgParam && campaign && campaign.indexOf("_") > -1) {
                        isParamCorrect = isParamCorrect && extGlobal.utils.extractParam(cmpgParam, url) === campaign.split("_")[1] && extGlobal.utils.extractParam("type", url) === campaign.split("_")[0]; //medianet case
                    } else {
                        isParamCorrect = isParamCorrect && extGlobal.utils.extractParam("type", url) === campaign; //regular case
                    }
                }
            } else if (partnerConfig.frCodeChrome) {
                var typeDefault = extGlobal.constants.distributionChannels[partner].typeDefault ? extGlobal.constants.distributionChannels[partner].typeDefault : extGlobal.constants.typeDefault;
                isParamCorrect = extGlobal.utils.extractParam("fr", url) === partnerConfig.frCodeChrome && extGlobal.utils.extractParam("type", url) === typeDefault;
            }
            return isPathCorrect && isParamCorrect;
        }

        function isNewTab(url) {
            return extGlobal.utils.extractParam("type", url) === "newtab";
        }

        function isMainFrame(reqDetails) {
            return reqDetails.type === "main_frame" && reqDetails.frameId === 0;
        }

        function shouldRedirect() {
            // if dynamicSearch set to true then webRequest should always redirect
            return extGlobal.constants.dynamicSearch;
        }

        function initiatorExists(details) {
            return details.initiator && details.initiator.length > 0;
        }

        function existsYlt(url) {
            var path = url ? url.substring(0, url.indexOf("?")) : "";
            return path.indexOf("_ylt=") > -1;
        }

        if (isMainFrame(details) &&
            shouldRedirect() &&
            !initiatorExists(details) &&
            !details.originUrl &&
            isYahooSearch(details.url) &&
            !existsYlt(details.url) &&
            !isNewTab(details.url) &&
            !isCorrectTrackingParams(details.url)) {
            var queryString = decodeURIComponent(extGlobal.utils.extractParam("p", details.url).replace(/\+/g,  " "));
            return {redirectUrl: extGlobal.utils.getSearchUrl(queryString, true)};
        }
    }, {
        urls: ["https://*.search.yahoo.com/search*", "https://*.search.yahoo.com/yhs/search*"] // List of URLs
    }, ["blocking"]); // Block intercepted requests until this handler has finished
}
