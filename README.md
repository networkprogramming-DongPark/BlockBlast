# 블록 매치

2인 대전 블록 퍼즐 게임입니다. 설계 내용은 [Common/docs/DESIGN.md](Common/docs/DESIGN.md)를 참고하세요.

| 폴더 | 역할 |
| --- | --- |
| `Common` | 공용 모델, 설계 문서 |
| `Client` | Java Swing 게임 클라이언트 |
| `Server` | 게임 서버 |

## 개발 환경 준비

```bash
git clone https://github.com/networkprogramming-DongPark/BlockBlast.git
```

IntelliJ IDEA에서 `BlockBlast` 폴더를 열면 `Common`, `Client`, `Server` 모듈이 함께 불러와집니다. JDK를 찾지 못한다는 안내가 나오면 Project Structure에서 JDK 17 이상을 지정합니다.

## 실행

IntelliJ에서는 `blockmatch.server.ServerMain`을 먼저 실행한 뒤 `blockmatch.client.ClientMain`을 두 번 실행합니다. 두 번째 실행을 위해 Run Configuration에서 Allow multiple instances를 켭니다.

명령줄에서는 저장소 최상위 폴더에서 다음과 같이 실행합니다. (Git Bash 기준, JDK 17 이상)

```bash
javac -encoding UTF-8 -d out $(find Common/src Client/src Server/src -name '*.java')
java -cp out blockmatch.server.ServerMain
```

터미널을 두 개 더 열어 클라이언트를 각각 실행합니다. 실행 인자는 `[닉네임] [서버 주소]`이고, 서버 주소를 생략하면 `localhost`에 접속합니다.

```bash
java -cp out blockmatch.client.ClientMain 동오
```

```bash
java -cp out blockmatch.client.ClientMain 성훈
```

접속 상태는 게임 창의 채팅 영역에, 주고받은 메시지는 서버 콘솔에 표시됩니다. 지금은 입장과 게임 시작 메시지까지만 구현되어 있습니다.

게임 창에서는 내 보드 아래의 블록을 마우스로 끌어 내 보드에 놓을 수 있습니다. 가로나 세로 줄을 채우면 그 줄이 지워지고 점수가 오릅니다. 상대 보드와 점수는 아직 서버와 연결되지 않아 비어 있습니다.

## Wireshark로 통신 확인

1. Wireshark에서 `Adapter for loopback traffic capture`를 선택합니다. 서버와 클라이언트를 다른 PC에서 실행할 때는 실제 네트워크 어댑터를 선택합니다.
2. 디스플레이 필터에 `tcp.port == 30000`을 입력합니다.
3. 서버와 클라이언트 두 개를 실행합니다.
4. 패킷을 우클릭해 Follow > TCP Stream을 선택하고, 인코딩을 UTF-8로 지정하면 메시지를 읽을 수 있습니다.
