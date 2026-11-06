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

IntelliJ에서는 `blockmatch.client.ClientMain`을 실행합니다.

명령줄에서는 저장소 최상위 폴더에서 다음과 같이 실행합니다. (Git Bash 기준, JDK 17 이상)

```bash
javac -encoding UTF-8 -d out $(find Common/src Client/src Server/src -name '*.java')
java -cp out blockmatch.client.ClientMain
```
