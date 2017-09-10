Foundation.Magellan.defaults.animationDuration = 200;
Foundation.Magellan.defaults.barOffset = -20;
$(document).foundation();

$(window).load(function() {

  // Recalculate sticky positions
  function updatePositions(resize) {
    if(resize) $(window).trigger('resize');
    else $('.sticky').foundation('_calc', true);
  }

  // Initialize Mermaid
  if(typeof mermaidAPI !== 'undefined')  {
    mermaidAPI.initialize({
      //logLevel: 1,
      startOnLoad: false,
      cloneCssStyles: false
    });
    $(".mermaid_src").each(function() {
      var srcE = this;
      var e = srcE.parentElement;
      var insertSvg = function(svgCode, bindFunctions) {
        //console.log("Rendered "+e.id);
        e.innerHTML = svgCode;
        bindFunctions(e);
      };
      //console.log("Rendering "+e.id);
      mermaidAPI.render(e.id + "_svg", srcE.textContent, insertSvg, "#"+e.id);
      updatePositions();
    });
  }

  // Initialize multi-version navigation
  $(".a_vnav").each(function() {
    var panel = $(this);
    panel.find(".a_toggle").click(function() {
      panel.toggleClass("a_expanded");
      updatePositions(true);
      return false;
    });
  });
  var versionIdx = $("#_version_idx");
  if(versionIdx.size() > 0) {
    var helperA = $("#_site_root")[0];
    if(helperA.protocol !== "file:") {
      $(".a_vnav2").each(function() { // Activate vnav2 toggles
        var toggle = $(this);
        var pane = $("#"+toggle.attr("data-toggle"));
        new Foundation.Dropdown(pane, { vOffset: 0 });
        pane.on("show.zf.dropdown", function() { toggle.addClass("a_expanded"); });
        pane.on("hide.zf.dropdown", function() { toggle.removeClass("a_expanded"); });
        toggle.addClass("a_vnav2_toggle");
      });
      var siteRoot = helperA.href;
      helperA.href = siteRoot + "../";
      var siteParent = helperA.href;
      //console.log("siteRoot: " + siteRoot);
      //console.log("siteParent: " + siteParent);
      function normalize(u) {
        var re = /^([a-zA-Z0-9-_.]+)\/$/;
        var p =  u.startsWith(siteParent) ? u.substring(siteParent.length) : null;
        if(p !== null) {
          var a = p.replace(/[?#].*/, "").match(re);
          return a !== null ? a[1] : null;
        } else return null;
      }
      var thisVersion = normalize(siteRoot);
      //console.log("thisVersion: " + thisVersion);
      $(".a_currentversion").text(thisVersion);
      $(".a_vnav").css("visibility", "visible");
      var versionIdxFrame = document.createElement("iframe");
      versionIdxFrame.src = versionIdx[0].href;
      versionIdx.append(versionIdxFrame);
      $(versionIdxFrame).load(function() {
        var versions = $(versionIdxFrame).contents().find("a[href]").map(function() {
          return normalize(this.href);
        }).toArray().sort();
        if(versions.length == 0) versions = [thisVersion];
        //console.log("versions: " + versions);
        var pageLoc = document.location.href;
        var pageLocal = pageLoc.startsWith(siteRoot) ? pageLoc.substring(siteRoot.length) : null;
        //console.log("pageLocal: " + pageLocal);
        var ul = document.createElement("ul");
        var container = $(".a_vnav > div, .a_vnav2_pane span");
        $(versions).each(function() {
          if(this == thisVersion) {
            var a = document.createElement("span");
          } else {
            var a = document.createElement("a");
            var baseTarget = siteParent + this + "/";
            var pageTarget = baseTarget + (pageLocal === null ? "" : pageLocal);
            a.href = pageTarget;
            $(a).click(function() {
              $.ajax({
                type: 'HEAD',
                url: pageTarget,
                success: function() { window.location.href = pageTarget; },
                error: function() { window.location.href = baseTarget; },
                global: false
              });
              return false;
            });
          }
          a.innerText = this;
          var li = document.createElement("li");
          li.appendChild(a);
          ul.appendChild(li);
        });
        container.empty();
        container.append(ul);
        updatePositions(true);
      });
    }
  }

});
