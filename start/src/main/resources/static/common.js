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

// password
const inputField = document.getElementById('password');
function saveInput() {
    const inputValue = inputField.value;
    localStorage.setItem('password', inputValue);
}
function loadInput() {
    const savedValue = localStorage.getItem('password');
    if (savedValue) {
        inputField.value = savedValue;
    }
}
inputField.onchange = saveInput;
$(document).ready(function() {
    loadInput();
});