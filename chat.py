import time
import xmltodict
from envs.pythonProject2.Lib import xml
from flask import Flask
from flask import request
import hashlib
import re
import xml.etree.ElementTree as ET

from chatbot_graph import *

app= Flask(__name__)

@app.route('/',methods=["GET", "POST"])

# def reply_text(to_user, from_user, content):
#     """
#     以文本类型的方式回复请求
#     """
#     return """
#         <xml>
#             <ToUserName><![CDATA[{}]]></ToUserName>
#             <FromUserName><![CDATA[{}]]></FromUserName>
#             <CreateTime>{}</CreateTime>
#             <MsgType><![CDATA[text]]></MsgType>
#             <Content><![CDATA[{}]]></Content>
#         </xml>
#         """.format(to_user, from_user, int(time.time() * 1000), content)

def wechat():#1、 获取携带的 signature、timestamp、nonce、echostr
    if request.method == 'GET':
        signature = request.args.get("signature", "")
        timestamp= request.args.get("timestamp", "")
        nonce= request.args.get("nonce", "")
        echostr= request.args.get("echostr", "")
        print(signature, timestamp, nonce, echostr)
        token="connect"
        #2、 进行字典排序
        data =[token, timestamp, nonce]
        data.sort()#3、三个参数拼接成一个字符串并进行sha1加密
        temp = ''.join(data)
        sha1= hashlib.sha1(temp.encode('utf-8'))
        hashcode=sha1.hexdigest()
        print(hashcode)#4、对比获取到的signature与根据上面token生成的hashcode，如果一致，则返回echostr，对接成功
        if hashcode ==signature:
            return echostr
        else:
            return "error"
    if request.method == 'POST':
        # 表示微信服务器转发消息过来
        xml_str = request.data
        xml = ET.fromstring(request.data)
        if not xml_str:
            return ""
        # 对xml字符串进行解析
        xml_dict = xmltodict.parse(xml_str)
        xml_dict = xml_dict.get("xml")
        # 提取消息类型
        msg_type = xml_dict.get("MsgType")
        content = xml.find('Content').text
        if msg_type == "text":
            # 表示发送的是文本消息
            # 构造返回值，经由微信服务器回复给用户的消息内容
            answer = handler.chat_main(content)
            resp_dict = {
                "xml": {
                    "ToUserName": xml_dict.get("FromUserName"),
                    "FromUserName": xml_dict.get("ToUserName"),
                    "CreateTime": int(time.time()),
                    "MsgType": "text",
                    "Content": answer,  # 这里是回复的内容哦！
                }
            }
            resp_xml_str = xmltodict.unparse(resp_dict)
            return resp_xml_str

# def index():
#     return '<h1>Hello World!</h1>'

if __name__ == '__main__':
     handler = ChatBotGraph()
     app.run(host='127.0.0.1', port=80, debug=True)