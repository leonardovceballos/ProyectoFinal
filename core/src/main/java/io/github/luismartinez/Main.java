package io.github.luismartinez;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.luismartinez.Entidades.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private Texture backgroundTexture;
    private Texture player_img;
    private Texture enemy_img;
    private Texture bullet_img;
    private Texture bullet_img_enemy;
    private Texture boss_img;
    private Texture remote_player_img;

    private Player player;

    private Vector2 touchPos;
    private Rectangle playerRectangle, bulletRectangle, enemyRectangle;

    private ArrayList<Enemy> enemies;
    private ArrayList<Bullet> player_bullets;
    private ArrayList<Explosion> explosiones;
    private ArrayList<BulletEnemy> enemy_bullets;
    private ArrayList<PowerUp> powerUps;

    private BitmapFont fontScore;

    Boss jefe;
    private ShapeRenderer barraHP;

    private final static int TIEMPO_DISPARO_PLAYER = 150;
    private final static int VELOCIDAD_PLAYER = 300;
    private final static int VELOCIDAD_SUPER = 600;
    private final static int TIEMPO_DISPARO_ENEMY = 250;
    private final static int SERVER_PORT = 12345;
    private final float POWERUP_SPAWN_INTERVAL = 15f;
    private final static long NETWORK_SEND_INTERVAL_MS = 50;
    private int vidas = 3;
    private int score;
    private int bossSpawnRate;
    private int bossHealth;
    private float enemySpawnTimer = 0;
    private float enemySpawnInterval = 2f;
    private float enemyShootTimer = 0f;
    private float powerUpSpawnTimer = 0;
    private float powerTimer = 0;
    private float tiempoPartida = 0;
    private float backgroundY = 0;
    private float backgroundSpeed = 5;
    private long disparo_Timer = 0;
    private long lastSendTime = 0;

    private boolean immune = false;
    private boolean superSpeed = false;
    private boolean fastBullets = false;

    private Pool<Bullet> playerBulletPool;
    private Pool<BulletEnemy> enemyBulletPool;

    private EnumMap<PowerType, Texture> powerTextures;
    private GameState currentState = GameState.MENU;
    private PowerType activePower = null;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String myClientId;
    private Map<String, RemotePlayer> remotePlayers;

    private static final String SERVER_IP = "localhost";




    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        viewport.apply();

        touchPos = new Vector2();
        playerRectangle = new Rectangle();
        bulletRectangle = new Rectangle();
        enemyRectangle = new Rectangle();

        backgroundTexture = new Texture("background.jpg");
        backgroundTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        player_img = new Texture("player.png");
        enemy_img = new Texture("yellow.png");
        bullet_img = new Texture("Bullet.png");
        bullet_img_enemy = new Texture("Alien_Bullet.png");
        boss_img = new Texture("purple.png");
        remote_player_img = new Texture("player.png");

        powerTextures = new EnumMap<>(PowerType.class);
        powerTextures.put(PowerType.IMMUNE, new Texture("inmune.png"));
        powerTextures.put(PowerType.SPEED, new Texture("pspeed.png"));
        powerTextures.put(PowerType.BULLET_SPEED, new Texture("bspeed.png"));
        powerTextures.put(PowerType.TRIPLE_POWER, new Texture("triple.png"));
        powerTextures.put(PowerType.EXTRA_LIFE, new Texture("extra.png"));

        player = new Player(new Vector2(400 - 32, 50), player_img, VELOCIDAD_PLAYER);
        player.getSprite().setSize(64, 64);

        enemies = new ArrayList<>();
        player_bullets = new ArrayList<>();
        explosiones = new ArrayList<>();
        enemy_bullets = new ArrayList<>();
        powerUps = new ArrayList<>();
        remotePlayers = new HashMap<>();

        Random posicion_random = new Random();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Nasa21-l23X.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 30;
        parameter.color = Color.WHITE;
        fontScore = generator.generateFont(parameter);

        barraHP = new ShapeRenderer();
        jefe = null;

        bossSpawnRate = 1000;
        bossHealth = 1000;

        playerBulletPool = new Pool<Bullet>(20, 100) {
            @Override
            protected Bullet newObject() {
                return new Bullet(new Vector2(0, 0), bullet_img, 0);
            }
        };

        enemyBulletPool = new Pool<BulletEnemy>(50, 200) { // Adjust capacities as needed for enemy bullets
            @Override
            protected BulletEnemy newObject() {
                return new BulletEnemy(new Vector2(), bullet_img_enemy, 0);
            }
        };

        connectToServer();

        // Música desactivada por ahora
        // hit = Gdx.audio.newMusic(Gdx.files.internal("hit.mp3"));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        switch (currentState) {
            case MENU:
                handleMenuInput();
                break;
            case RUNNING:
                input();
                logic();
                break;
            case PAUSED:
                handlePauseInput();
                break;
            case GAME_OVER:
                handleGameOverInput();
                break;
        }
        draw();
    }

    private void input() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            currentState = GameState.PAUSED;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            player.moveRight(delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            player.moveLeft(delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            player.getSprite().setCenterX(touchPos.x);
        }

        long tiempoDisparo = fastBullets ? TIEMPO_DISPARO_PLAYER / 2 : TIEMPO_DISPARO_PLAYER;
        if (System.currentTimeMillis() - disparo_Timer >= tiempoDisparo) {
            createPlayerBullet();
            disparo_Timer = System.currentTimeMillis();
        }
    }

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            resetGame();
            currentState = GameState.RUNNING;
        }
    }

    private void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            currentState = GameState.RUNNING;
        }
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
            currentState = GameState.RUNNING;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            currentState = GameState.MENU;
        }
    }

    private void logic() {
        float delta = Gdx.graphics.getDeltaTime();
        float worldWidth = viewport.getWorldWidth();

        if (currentState == GameState.RUNNING) {
            tiempoPartida += delta;

            if (clientSocket != null && out != null && System.currentTimeMillis() >= lastSendTime + NETWORK_SEND_INTERVAL_MS) {
                sendMyPosition();
                lastSendTime = System.currentTimeMillis();

            }
        }

        updatePlayerPosition(worldWidth);
        spawnEnemies(delta);
        updateEnemies(delta);
        updateBoss(delta);
        updatePlayerBullets(delta);
        shootEnemyBullets(delta);
        updateEnemyBullets(delta);
        updateExplosions(delta);

        powerUpSpawnTimer += delta;
        if (powerUpSpawnTimer >= POWERUP_SPAWN_INTERVAL) {
            spawnRandomPowerUp();
            powerUpSpawnTimer = 0;

        }

        if (jefe == null && score >= bossSpawnRate) {
            jefe = new Boss(
                new Vector2(400 - 64, 480),
                boss_img,
                30,
                bossHealth,
                viewport

            );
            jefe.getSprite().setSize(50, 50);
            bossSpawnRate += 10000;
            bossHealth += 1000;

        }
        updatePowerUps(delta);
        updateActivePowers(delta);

        synchronized (remotePlayers) {
            for (RemotePlayer rp : remotePlayers.values()) {
                rp.update(delta);

            }
        }
    }

    private void updatePlayerPosition(float worldWidth) {
        Sprite sprite = player.getSprite();
        sprite.setX(MathUtils.clamp(sprite.getX(), 0, worldWidth - sprite.getWidth()));
        playerRectangle.set(sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight());
    }

    private boolean updateSinglePlayerBullet(Bullet bullet, float delta) {
        bullet.update(delta);
        Sprite bSprite = bullet.getSprite();

        if (bSprite.getY() > viewport.getWorldHeight()) {
            playerBulletPool.free(bullet);
            return true;
        }
        return false;
    }

    private boolean handlePlayerBulletEnemyCollision(Bullet bullet) {
        Sprite bSprite = bullet.getSprite();
        bulletRectangle.set(bSprite.getX(), bSprite.getY(), bSprite.getWidth(), bSprite.getHeight());

        for (int j = enemies.size() - 1; j >= 0; j--) {
            Enemy enemy = enemies.get(j);
            Sprite eSprite = enemy.getSprite();
            if (bulletRectangle.overlaps(eSprite.getBoundingRectangle())) {
                explosiones.add(new Explosion(eSprite.getX(), eSprite.getY()));
                enemies.remove(j);
                score += 100;
                playerBulletPool.free(bullet);
                return true;
            }
        }
        return false; // Bullet did not hit any enemy
    }

    private boolean handlePlayerBulletBossCollision(Bullet bullet) {
        if (jefe != null && jefe.isAlive()) {
            Sprite bSprite = bullet.getSprite();
            bulletRectangle.set(bSprite.getX(), bSprite.getY(), bSprite.getWidth(), bSprite.getHeight());
            Sprite bossSprite = jefe.getSprite();

            if (bulletRectangle.overlaps(bossSprite.getBoundingRectangle())) {
                jefe.bossHit(30);
                explosiones.add(new Explosion(bossSprite.getX() + bossSprite.getWidth()/2, bossSprite.getY() + bossSprite.getHeight()/2));

                if (!jefe.isAlive()) {
                    explosiones.add(new Explosion(bossSprite.getX(), bossSprite.getY()));
                    score += 5000;
                    jefe = null;
                }
                playerBulletPool.free(bullet);
                return true;
            }
        }
        return false; // Bullet did not hit the boss
    }

    private void updatePlayerBullets(float delta) {
        for (int i = player_bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = player_bullets.get(i);

            boolean removed = false;

            if (updateSinglePlayerBullet(bullet, delta)) {
                removed = true;
            }

            if (!removed && handlePlayerBulletEnemyCollision(bullet)) {
                removed = true;
            }

            if (!removed && handlePlayerBulletBossCollision(bullet)) {
                removed = true;
            }

            if (removed) {
                player_bullets.remove(i);
            }
        }
    }

    private boolean updateAndCheckEnemyBulletBounds(BulletEnemy bullet, float delta) {
        bullet.update(delta);
        Sprite bSprite = bullet.getSprite();

        if (bSprite.getY() < -bSprite.getHeight()) {
            enemyBulletPool.free(bullet);
            return true;
        }
        return false;
    }

    private boolean handleEnemyBulletPlayerCollision(BulletEnemy bullet) {
        if (!player.isAlive()) {
            return false;
        }

        Sprite bSprite = bullet.getSprite();

        playerRectangle.set(player.getSprite().getX(), player.getSprite().getY(),
            player.getSprite().getWidth(), player.getSprite().getHeight());

        if (bSprite.getBoundingRectangle().overlaps(playerRectangle)) {
            enemyBulletPool.free(bullet);
            return true;
        }
        return false;
    }

    private void processPlayerHit() {
        if (activePower != PowerType.IMMUNE && activePower != PowerType.TRIPLE_POWER) {
            vidas--;
            explosiones.add(new Explosion(player.getSprite().getX(), player.getSprite().getY()));

            player.setVisible(false);
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                    player.setVisible(true);
                }
            }, 0.2f);


            if (vidas <= 0) {
                currentState = GameState.GAME_OVER;
            }
        }
    }

    private void updateEnemyBullets(float delta) {
        for (int i = enemy_bullets.size() - 1; i >= 0; i--) {
            BulletEnemy bullet = enemy_bullets.get(i);

            boolean bulletRemovedFromList = false;

            if (updateAndCheckEnemyBulletBounds(bullet, delta)) {
                enemy_bullets.remove(i);
                bulletRemovedFromList = true;
                continue;
            }

            if (handleEnemyBulletPlayerCollision(bullet)) {
                enemy_bullets.remove(i);
                bulletRemovedFromList = true;

                processPlayerHit();

                if (currentState == GameState.GAME_OVER) {
                    return;
                }
                continue;
            }
        }
    }
    private void updateExplosions(float delta) {
        for (int i = explosiones.size() - 1; i >= 0; i--) {
            Explosion ex = explosiones.get(i);
            ex.update(delta);
            if (ex.isComplete()) {
                explosiones.remove(i);
            }
        }
    }

    private void updateEnemies(float delta) {

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Sprite eSprite = enemies.get(i).getSprite();
            eSprite.translateY(-60 * delta);
            enemyRectangle.set(eSprite.getX(), eSprite.getY(), eSprite.getWidth(), eSprite.getHeight());

            if (eSprite.getY() < -eSprite.getHeight()) {
                enemies.remove(i);
                score -= 100;
                if (score < 0) {
                    currentState = GameState.GAME_OVER;
                    resetGame();
                }
            } else if (playerRectangle.overlaps(enemyRectangle)) {
                enemies.remove(i);
                vidas--;
                player.setVisible(false);
                explosiones.add(new Explosion(player.getSprite().getX(), player.getSprite().getY()));
                if (vidas <= 0) {
                    currentState = GameState.GAME_OVER;
                    resetGame();
                }
            }
        }
    }

    private void updateBoss(float deltaTime) {
        if (jefe == null) return;

        if (jefe.isAlive()) {
            jefe.update(deltaTime);

            if (jefe.canShoot()) {
                enemy_bullets.add(new BulletEnemy(
                    new Vector2(jefe.getPosition().x + jefe.getSprite().getWidth() / 2f, jefe.getPosition().y),
                    bullet_img_enemy,
                    200
                ));
                jefe.setLastShoot(System.currentTimeMillis());
            }
        } else {
            jefe = null;
        }
    }

    private void updatePowerUps(float delta) {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp pu = powerUps.get(i);
            pu.getSprite().translateY(-50 * delta);

            if (pu.getSprite().getBoundingRectangle().overlaps(playerRectangle)) {
                applyPower(pu.getType());
                powerUps.remove(i);
            } else if (pu.getSprite().getY() < -pu.getSprite().getHeight()) {
                powerUps.remove(i);
            }
        }
    }

    private void updateActivePowers(float delta) {
        if (activePower == null) return;

        powerTimer -= delta;
        if (powerTimer <= 0) {
            immune = false;
            fastBullets = false;
            superSpeed = false;
            player.setSpeed(VELOCIDAD_PLAYER);
            activePower = null;
        }
    }

    private void spawnEnemies(float delta) {
        enemySpawnTimer += delta;
        if (enemySpawnTimer > enemySpawnInterval) {
            enemySpawnTimer = 0;
            createEnemy();

            if (enemySpawnInterval > 0.5f) {
                enemySpawnInterval -= 0.05f;
            }
        }
    }

    private void spawnRandomPowerUp() {
        float x = MathUtils.random(0f, viewport.getWorldWidth() - 32);
        float y = viewport.getWorldHeight();

        PowerType[] types = PowerType.values();
        PowerType randomType = types[MathUtils.random(types.length - 1)];

        Texture texture = powerTextures.get(randomType);
        PowerUp pu = new PowerUp(new Vector2(x, y), texture, randomType);
        powerUps.add(pu);
    }

    private void createPlayerBullet() {
        float bulletWidth = 8;
        float bulletHeight = 16;
        float bulletSpeed = fastBullets ? 200 : 150;

        float xCenter = player.getSprite().getX() + player.getSprite().getWidth() / 2f - bulletWidth / 2f;
        float yStart = player.getSprite().getY() + player.getSprite().getHeight();

        Bullet bullet = playerBulletPool.obtain();
        bullet.setPosition(new Vector2(xCenter, yStart));
        bullet.setSpeed(bulletSpeed);
        bullet.getSprite().setSize(bulletWidth, bulletHeight);
        bullet.setIsAlive(true);
        bullet.setVisible(true);
        player_bullets.add(bullet);

        if (activePower == PowerType.TRIPLE_POWER) {
            Bullet left = playerBulletPool.obtain();
            left.setPosition(new Vector2(xCenter - 20, yStart));
            left.setSpeed(bulletSpeed);
            left.getSprite().setSize(bulletWidth, bulletHeight);
            left.setIsAlive(true);
            left.setVisible(true);
            player_bullets.add(left);

            Bullet right = playerBulletPool.obtain();
            right.setPosition(new Vector2(xCenter + 20, yStart));
            right.setSpeed(bulletSpeed);
            right.getSprite().setSize(bulletWidth, bulletHeight);
            right.setIsAlive(true);
            right.setVisible(true);
            player_bullets.add(right);
        }
    }

    private void createEnemy() {
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        float x = MathUtils.random(0f, worldWidth - 48);
        Enemy enemy = new Enemy(new Vector2(x, worldHeight), enemy_img, 50);
        enemy.getSprite().setSize(48, 48);

        enemies.add(enemy);
    }

    private void createEnemyBullet(Enemy e) {
        float bulletWidth = 8;
        float bulletHeight = 16;
        float bulletSpeed = 100;

        BulletEnemy bullet = enemyBulletPool.obtain();

        bullet.setPosition(new Vector2(
            e.getSprite().getX() + e.getSprite().getWidth() / 2f - bulletWidth / 2f,
            e.getSprite().getY()
        ));
        bullet.setSpeed(bulletSpeed);
        bullet.getSprite().setSize(bulletWidth, bulletHeight);
        bullet.setIsAlive(true);
        bullet.setVisible(true);

        enemy_bullets.add(bullet);
        }

    private void shootEnemyBullets(float delta) {
        enemyShootTimer += delta;
        if (enemyShootTimer > TIEMPO_DISPARO_ENEMY / 250f) {
            enemyShootTimer = 0;
            for (Enemy e : enemies) {
                createEnemyBullet(e);
            }
        }
    }

    private void applyPower(PowerType type) {
        if (type == PowerType.EXTRA_LIFE) {
            vidas++;
            return;
        }

        activePower = type;
        powerTimer = 15f;

        immune = false;
        fastBullets = false;
        superSpeed = false;
        player.setSpeed(VELOCIDAD_PLAYER);

        switch (type) {
            case IMMUNE:
                immune = true;
                break;
            case SPEED:
                superSpeed = true;
                player.setSpeed(VELOCIDAD_SUPER);
                break;
            case BULLET_SPEED:
                fastBullets = true;
                break;
            case TRIPLE_POWER:
                immune = true;
                fastBullets = true;
                superSpeed = true;
                player.setSpeed(VELOCIDAD_SUPER);
                break;
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        drawBackground(Gdx.graphics.getDeltaTime());

        switch (currentState) {
            case MENU:
                drawMenu();
                batch.end();
                return;
            case RUNNING:
                drawGameplay();
                batch.end();
                if (jefe != null && barraHP != null && jefe.isAlive()) {
                    float barWidth = 200;
                    float barHeight = 15;
                    float margin = 10;

                    float barX = margin;
                    float barY = margin;

                    drawHPBar(barX, barY, barWidth, barHeight);
                }
                return;
            case PAUSED:
                drawGameplay();
                drawPauseScreen();
                batch.end();
                return;
            case GAME_OVER:
                drawGameOver();
                break;
        }

        batch.end();
    }

    private void drawMenu() {
        fontScore.draw(batch, "PRESIONA ENTER PARA EMPEZAR", 220, 250);
    }

    private void drawGameOver() {
        fontScore.draw(batch, "GAME OVER", 330, 260);
        fontScore.draw(batch, "PRESIONA R PARA REINICIAR", 230, 200);
        fontScore.draw(batch, "PRESIONA M PARA VOLVER AL MENU", 200, 160);
    }

    private void drawGameplay() {
        if (player.isVisible()) {
            player.getSprite().draw(batch);
        }

        synchronized (remotePlayers) {
            for (RemotePlayer rp : remotePlayers.values()) {
                rp.getSprite().draw(batch);
            }
        }

        if (jefe != null && jefe.isAlive()) {
            jefe.getSprite().draw(batch);
        }

        for (Enemy e : enemies) {
            e.getSprite().draw(batch);
        }

        for (Bullet b : player_bullets) {
            b.getSprite().draw(batch);
        }

        for (Bullet b : enemy_bullets) {
            b.getSprite().draw(batch);
        }

        for (Explosion ex : explosiones) {
            ex.draw(batch, Gdx.graphics.getDeltaTime());
        }

        for (PowerUp p : powerUps) {
            p.getSprite().draw(batch);
        }

        fontScore.draw(batch, "Score: " + score, 380, 470);
        fontScore.draw(batch, "Vidas: " + vidas, 10, 470);
        fontScore.draw(batch, "Tiempo: " + (int) tiempoPartida, 10, 440);

        if (activePower != null) {
            Texture icon = powerTextures.get(activePower);
            float size = 40;
            float margin = 10;

            float iconX = viewport.getWorldWidth() - size - margin;
            float iconY = viewport.getWorldHeight() - size - margin;

            float textX = iconX - 40;
            float textY = iconY + size - 5;

            batch.draw(icon, iconX, iconY, size, size);
            fontScore.draw(batch, String.format("%.0f", powerTimer), textX, textY);
        }

    }

    private void drawHPBar(float x, float y, float width, float height) {
        float hp = jefe.getHp();
        float maxHp = 1000;

        barraHP.setProjectionMatrix(batch.getProjectionMatrix());
        barraHP.begin(ShapeRenderer.ShapeType.Filled);

        barraHP.setColor(Color.WHITE);
        barraHP.rect(x, y, width, height);

        float hpWidth = (hp / maxHp) * width;
        barraHP.setColor(Color.RED);
        barraHP.rect(x, y, hpWidth, height);

        barraHP.end();
    }


    private void drawPauseScreen() {
        fontScore.draw(batch, "JUEGO EN PAUSA", 300, 260);
        fontScore.draw(batch, "PRESIONA ESC PARA CONTINUAR", 230, 220);
    }

    private void drawBackground(float delta) {
        backgroundY -= backgroundSpeed * delta;

        if (backgroundY <= -viewport.getWorldHeight()) {
            backgroundY = 0;
        }

        batch.draw(backgroundTexture, 0, backgroundY, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.draw(backgroundTexture, 0, backgroundY + viewport.getWorldHeight(), viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    private void resetGame() {
        vidas = 3;
        score = 0;
        tiempoPartida = 0;
        immune = false;
        fastBullets = false;
        superSpeed = false;
        enemySpawnInterval = 2f;

        player.setPosition(new Vector2(400 - 32, 50));
        player.setVisible(true);

        enemies.clear();
        player_bullets.clear();
        enemy_bullets.clear();
        explosiones.clear();
        if (jefe != null) {
            jefe.setIsAlive(false);
            jefe.resetHp();
        }
        jefe = null;

        bossSpawnRate = 1000;
        bossHealth = 1000;
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                clientSocket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                System.out.println("Connected to game server.");

                myClientId = clientSocket.getLocalAddress().getHostAddress() + ":" + clientSocket.getLocalPort();
                System.out.println("My Client ID: " + myClientId);

                new Thread(this::listenForServerMessages).start();

            } catch (IOException e) {
                System.err.println("Could not connect to server: " + e.getMessage());

            }
        }).start();
    }
    private void listenForServerMessages() {
        try {
            String serverMessage;

            while ((serverMessage = in.readLine()) != null) {
                String[] parts = serverMessage.split(":");

                if (parts.length == 4) {
                    String id = parts[0] + ":" + parts[1];
                    float x = Float.parseFloat(parts[2]);
                    float y = Float.parseFloat(parts[3]);

                    if (!id.equals(myClientId)) {
                        Gdx.app.postRunnable(() -> {
                            RemotePlayer rp = remotePlayers.get(id);
                            if (rp == null) {
                                rp = new RemotePlayer(id, new Vector2(x, y), remote_player_img);
                                rp.getSprite().setSize(64, 64);
                                remotePlayers.put(id, rp);
                                System.out.println("New remote player joined: " + id);
                            }
                            rp.setPosition(new Vector2(x, y));
                        });
                    }
                } else {
                    System.out.println("Received malformed message: " + serverMessage);

                }
            }
        } catch (IOException e) {
            System.err.println("Error listening for server messages: " + e.getMessage());
        } finally {
            cleanupNetwork();
        }
    }

    private void sendMyPosition() {
        if (out != null && clientSocket.isConnected()) {
            out.println(player.getPosition().x + ":" + player.getPosition().y);
        }
    }

    private void cleanupNetwork() {
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) out.close();
        if (in != null) {
            try { in.close(); } catch (IOException e) { e.printStackTrace(); }
        }
        clientSocket = null;
        out = null;
        in = null;
        remotePlayers.clear();
        System.out.println("Network resources cleaned up.");
    }

    @Override
    public void dispose() {
        batch.dispose();
        player_img.dispose();
        enemy_img.dispose();
        bullet_img.dispose();
        bullet_img_enemy.dispose();
        boss_img.dispose();
        fontScore.dispose();
        backgroundTexture.dispose();
        barraHP.dispose();

        for (Texture tex : powerTextures.values()) {
            tex.dispose();
        }
    }

}
