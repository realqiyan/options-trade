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
      element.render('collapse');
      // element.render();
    });
}

function sideMapping(side){
    const sideMap = {
        '1': '买',
        '2': '卖',
        '3': '卖空',
        '4': '买回',
    };
    return sideMap[side] || '未知';
}

function statusMapping(status) {
    const statusMap = {
        '-1': '未知', // 未知状态
        '1': '待提交', // 等待提交
        '2': '提交中', // 提交中
        '5': '已提交', // 已提交，等待成交
        '10': '部分成交', // 部分成交
        '11': '全部成交', // 全部已成
        '14': '部分撤单', // 部分成交，剩余部分已撤单
        '15': '已撤单', // 全部已撤单，无成交
        '21': '下单失败', // 下单失败，服务拒绝
        '22': '已失效', // 已失效
        '23': '已删除', // 已删除，无成交的订单才能删除
        '24': '成交撤销', // 成交被撤销
        '25': '提前指派' // 提前指派
    };
    return statusMap[status] || '未知';
}

function extractDate(dateString) {
    // 使用 split 方法分割字符串"2025-02-21 00:00:00"
    const parts = dateString.split(' ')[0];
    return parts;
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
if (inputField){
    inputField.onchange = saveInput;
    $(document).ready(function() {
        loadInput();
    });
}