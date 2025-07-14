起動時に以下のエラー
\[21:23:58 ERROR]: Error loading saved data: raids

java.io.EOFException: Unexpected end of ZLIB input stream

&nbsp;	at java.util.zip.InflaterInputStream.fill(InflaterInputStream.java:244) ~\[?:?]

&nbsp;	at java.util.zip.InflaterInputStream.read(InflaterInputStream.java:158) ~\[?:?]

&nbsp;	at java.util.zip.GZIPInputStream.read(GZIPInputStream.java:117) ~\[?:?]

&nbsp;	at net.minecraft.util.FastBufferedInputStream.fill(FastBufferedInputStream.java:94) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at net.minecraft.util.FastBufferedInputStream.read(FastBufferedInputStream.java:25) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at java.io.DataInputStream.readUnsignedByte(DataInputStream.java:288) ~\[?:?]

&nbsp;	at java.io.DataInputStream.readByte(DataInputStream.java:268) ~\[?:?]

&nbsp;	at net.minecraft.nbt.NbtIo.readUnnamedTag(NbtIo.java:285) ~\[?:?]

&nbsp;	at net.minecraft.nbt.NbtIo.read(NbtIo.java:238) ~\[?:?]

&nbsp;	at net.minecraft.nbt.NbtIo.readCompressed(NbtIo.java:58) ~\[?:?]

&nbsp;	at net.minecraft.world.level.storage.DimensionDataStorage.readTagFromDisk(DimensionDataStorage.java:89) ~\[?:?]

&nbsp;	at net.minecraft.world.level.storage.DimensionDataStorage.readSavedData(DimensionDataStorage.java:65) ~\[?:?]

&nbsp;	at net.minecraft.world.level.storage.DimensionDataStorage.get(DimensionDataStorage.java:53) ~\[?:?]

&nbsp;	at net.minecraft.world.level.storage.DimensionDataStorage.computeIfAbsent(DimensionDataStorage.java:39) ~\[?:?]

&nbsp;	at net.minecraft.server.level.ServerLevel.<init>(ServerLevel.java:583) ~\[?:?]

&nbsp;	at net.minecraft.server.MinecraftServer.loadWorld0(MinecraftServer.java:602) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at net.minecraft.server.MinecraftServer.loadLevel(MinecraftServer.java:437) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at net.minecraft.server.dedicated.DedicatedServer.initServer(DedicatedServer.java:308) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:1104) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at net.minecraft.server.MinecraftServer.lambda$spin$0(MinecraftServer.java:320) ~\[paper-1.19.4.jar:git-Paper-550]

&nbsp;	at java.lang.Thread.run(Thread.java:840) ~\[?:?]

\[21:23:58 INFO]: Preparing start region for dimension minecraft:overworld

以下のコマンドを使用するとさーばーがクラッシュ
\[21:25:03 INFO]: kumo\_0621 joined the game

\[21:25:03 INFO]: kumo\_0621\[/127.0.0.1:53543] logged in with entity id 19 at (\[world]-252.6611518035077, 15.754372898122723, 15.323924222733103)

\[21:25:18 INFO]: kumo\_0621 issued server command: /physxmc ramp create 20 3 6 0.5 QUARTZ\_BLOCK

ターゲット VM から切断されました。アドレス: '127.0.0.1:53407'、トランスポート: 'ソケット'



プロセスは終了コード -1073740940 (0xC0000374) で終了しました

