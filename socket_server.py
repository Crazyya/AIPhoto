import os
import signal
import socketserver
import threading
import time
import random
from time import ctime

import globalvar

host = '192.168.7.88'
port = 52828
filePort = 52827
bufsize = 1024
addr = (host, port)
fileAddr = (host, filePort)
imagePathKey = "image_path"


def kill_process(p):
    try:
        pid = \
            os.popen("netstat -nlp | grep :%s | awk '{print $7}' | awk -F\" / \" '{ print $1 }'" % p).read().split(
                '/')[0]
        if pid != '':
            os.kill(int(pid), signal.SIGKILL)
    except:
        pass


def judgmentPath(path):
    """
    判断路劲是否存在，不存在就创建

    :param path:判断的路径
    :return:路径
    """
    if not os.path.exists(path):
        os.makedirs(path)
    return path


class MyFileServer(socketserver.BaseRequestHandler):
    def setup(self):
        print('file setup')

    def finish(self):
        print('file finish')

    def handle(self):
        conn = self.request
        try:
            data = bytes.decode(conn.recv(bufsize), encoding='utf8')
            if 'fileput' in data:
                print('接受文件')
                # 传输文件
                file_data = data.split(' ')
                file_size = int(file_data[1])  # 文件大小
                file_name = str(file_data[2])  # 文件名
                file_folder = time.strftime('%Y-%m-%d')

                folder_path = judgmentPath('/home/pi/output/' + file_folder)
                folder_path = judgmentPath(folder_path + '/' + globalvar.get_value(imagePathKey, 'notBarCode'))

                file_path = os.path.join(folder_path, file_name)  # 文件保存路径
                # 开始接收
                recvd_size = 0
                print('大小:{0};文件名:{1};文件夹:{2}开始接收文件'.format(str(file_size), file_name, file_folder))
                with open(file_path, 'wb') as file:
                    while not recvd_size == file_size:
                        if file_size - recvd_size > bufsize:
                            rdata = conn.recv(bufsize)
                            recvd_size += len(rdata)
                        else:
                            rdata = conn.recv(file_size - recvd_size)
                            recvd_size = file_size
                        file.write(rdata)
                print('文件写入完成')
                conn.send('done\n'.encode(encoding='utf8'))
            else:
                conn.send('啥玩意儿?\n'.encode(encoding='utf8'))
        except Exception as e:
            print('连接关闭:' + str(e))
        finally:
            conn.close()


class MyServer(socketserver.BaseRequestHandler):

    def setup(self):
        self.device_name = random.randint(10000, 99999)
        globalvar.set_device(self.device_name,'')
        print('msg setup :' + str(self.device_name))
        # myStateThread(self.request).start()

    def mSend(self, msg):
        sendMsg = msg + '\n'
        self.request.send(sendMsg.encode(encoding='utf8'))

    def handle(self):
        conn = self.request
        msg = "hi"
        while True:
            try:
                # 获得信息
                data = bytes.decode(conn.recv(bufsize), encoding='utf8')
                # print('接受到的:' + data)
                sm = globalvar.get_device(self.device_name, '')
                # print('指令:' + sm)
                if sm != '':
                    self.mSend(sm)
                    print('发送：' + sm)
                    globalvar.set_device(self.device_name, '')
                elif not data or data == 'exit':
                    # 空或exit就断开退出
                    break
                elif data == "hi":
                    self.mSend(msg)
                    time.sleep(1)
                else:
                    self.mSend('[{0}] {1}'.format(ctime(), '你发啥玩意儿呢！'))
            except ConnectionResetError as e:
                conn.close()
                print('连接关闭:' + str(e))
                break

    def finish(self):
        print('msg finish')
        pass


class myThread(threading.Thread):
    def __init__(self, func):
        super().__init__()
        self.func = func

    def run(self):
        self.func()


def startSocket():
    kill_process(port)
    server = socketserver.ThreadingTCPServer(addr, MyServer)
    try:
        server.serve_forever()
    except Exception as e:
        print(e)
    finally:
        server.server_close()


def startFileSocket():
    kill_process(filePort)
    serverf = socketserver.ThreadingTCPServer(fileAddr, MyFileServer)
    try:
        serverf.serve_forever()
    except Exception as e:
        print(e)
    finally:
        serverf.server_close()
