//JS
var $ = layui.$;
var element = layui.element;
var util = layui.util;
var laytpl = layui.laytpl;

function render(){
    layui.use(['element', 'layer', 'util', 'laytpl'], function(){
      var element = layui.element;
      var layer = layui.layer;
      var util = layui.util;
      var laytpl = layui.laytpl;
      var $ = layui.$;
      element.render('nav');
    });
}

// totp
const inputField = document.getElementById('totp');
function saveInput() {
    const inputValue = inputField.value;
    localStorage.setItem('totp', inputValue);
}
function loadInput() {
    const savedValue = localStorage.getItem('totp');
    if (savedValue) {
        inputField.value = savedValue;
    }
}
inputField.onchange = saveInput;
$(document).ready(function() {
    loadInput();
    // 清除历史记录
    localStorage.removeItem('password');
});