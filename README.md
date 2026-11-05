# 블록 매치 - Common

2인 협동 블록 퍼즐 게임의 공용 모델과 설계 문서를 담은 저장소입니다. 설계 내용은 [docs/DESIGN.md](docs/DESIGN.md)를 참고하세요.

## 개발 환경 준비

세 저장소를 같은 폴더 아래에 받습니다.

```bash
mkdir networkprogramming-DongPark
cd networkprogramming-DongPark
git clone https://github.com/networkprogramming-DongPark/Common.git
git clone https://github.com/networkprogramming-DongPark/Client.git
git clone https://github.com/networkprogramming-DongPark/Server.git
```

IntelliJ IDEA에서 `networkprogramming-DongPark` 폴더를 연 뒤, Project Structure의 Modules에서 `Common/Common.iml`, `Client/Client.iml`, `Server/Server.iml`을 가져옵니다.

## 실행

IntelliJ에서는 `blockmatch.client.ClientMain`을 실행합니다.

명령줄에서는 저장소들의 상위 폴더에서 다음과 같이 실행합니다. (Git Bash 기준, JDK 17 이상)

```bash
javac -encoding UTF-8 -d out $(find Common/src Client/src Server/src -name '*.java')
java -cp out blockmatch.client.ClientMain
```
