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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.luismartinez.Entidades.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

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
    private final float POWERUP_SPAWN_INTERVAL = 15f;
    private int vidas = 3;
    private int score;
    private float enemySpawnTimer = 0;
    private float enemySpawnInterval = 2f;
    private float enemyShootTimer = 0f;
    private float powerUpSpawnTimer = 0;
    private float powerTimer = 0;
    private float tiempoPartida = 0;
    private float backgroundY = 0;
    private float backgroundSpeed = 5;
    private long disparo_Timer = 0;

    private boolean immune = false;
    private boolean superSpeed = false;
    private boolean fastBullets = false;

    private EnumMap<PowerType, Texture> powerTextures;
    private GameState currentState = GameState.MENU;
    private PowerType activePower = null;

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

        Random posicion_random = new Random();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Nasa21-l23X.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 30;
        parameter.color = Color.WHITE;
        fontScore = generator.generateFont(parameter);

        barraHP = new ShapeRenderer();
        jefe = null;


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
            viewport.unproject(touchPos); // ajustamos a coordenadas del mundo
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
        }

        player.setVisible(true);

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

        if (jefe == null && score >= 2000) {
            jefe = new Boss(
                new Vector2(player.getPosition().x, player.getPosition().y + 100),
                boss_img,
                30,
                100
            );
        }

        updatePowerUps(delta);
        updateActivePowers(delta);
    }

    private void updatePlayerPosition(float worldWidth) {
        Sprite sprite = player.getSprite();
        sprite.setX(MathUtils.clamp(sprite.getX(), 0, worldWidth - sprite.getWidth()));
        playerRectangle.set(sprite.getX(), sprite.getY(), sprite.getWidth(), sprite.getHeight());
    }

    private void updatePlayerBullets(float delta) {
        for (int i = player_bullets.size() - 1; i >= 0; i--) {
            Sprite bSprite = player_bullets.get(i).getSprite();
            bSprite.translateY(200 * delta);
            bulletRectangle.set(bSprite.getX(), bSprite.getY(), bSprite.getWidth(), bSprite.getHeight());

            if (bSprite.getY() > 480) {
                player_bullets.remove(i);
                continue;
            }

            for (int j = enemies.size() - 1; j >= 0; j--) {
                Sprite eSprite = enemies.get(j).getSprite();
                if (bulletRectangle.overlaps(eSprite.getBoundingRectangle())) {
                    explosiones.add(new Explosion(eSprite.getX(), eSprite.getY()));
                    enemies.remove(j);
                    player_bullets.remove(i);
                    score += 100;
                    break;
                }
            }
        }
    }

    private void updateEnemyBullets(float delta) {
        for (int i = enemy_bullets.size() - 1; i >= 0; i--) {
            Sprite bSprite = enemy_bullets.get(i).getSprite();
            bSprite.translateY(-100 * delta);

            if (bSprite.getY() < -bSprite.getHeight()) {
                enemy_bullets.remove(i);
                continue;
            }

            if (bSprite.getBoundingRectangle().overlaps(playerRectangle)) {
                enemy_bullets.remove(i);

                if (activePower != PowerType.IMMUNE && activePower != PowerType.TRIPLE_POWER) {
                    vidas--;
                    player.setVisible(false);
                    explosiones.add(new Explosion(player.getSprite().getX(), player.getSprite().getY()));
                    if (vidas <= 0) {
                        currentState = GameState.GAME_OVER;
                        return;
                    }
                }
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
        float bulletSpeed = fastBullets ? 400 : 150;

        float xCenter = player.getSprite().getX() + player.getSprite().getWidth() / 2f - bulletWidth / 2f;
        float yStart = player.getSprite().getY() + player.getSprite().getHeight();

        Bullet bullet = new Bullet(new Vector2(xCenter, yStart), bullet_img, bulletSpeed);
        bullet.getSprite().setSize(bulletWidth, bulletHeight);
        player_bullets.add(bullet);

        if (activePower == PowerType.TRIPLE_POWER) {
            Bullet left = new Bullet(new Vector2(xCenter - 20, yStart), bullet_img, bulletSpeed);
            Bullet right = new Bullet(new Vector2(xCenter + 20, yStart), bullet_img, bulletSpeed);
            left.getSprite().setSize(bulletWidth, bulletHeight);
            right.getSprite().setSize(bulletWidth, bulletHeight);
            player_bullets.add(left);
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
        BulletEnemy bullet = new BulletEnemy(
            new Vector2(
                e.getSprite().getX() + e.getSprite().getWidth() / 2f - bulletWidth / 2f,
                e.getSprite().getY()),
            bullet_img_enemy,
            50
        );
        bullet.getSprite().setSize(bulletWidth, bulletHeight);
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
                    float barX = player.getPosition().x;
                    float barY = player.getPosition().y - 20;
                    drawHPBar(barX, barY, 60, 8);
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

        if (jefe != null) {
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
        float maxHp = 100;

        barraHP.setProjectionMatrix(batch.getProjectionMatrix());
        barraHP.begin(ShapeRenderer.ShapeType.Filled);

        // Fondo (gris)
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

    @Override
    public void dispose() {
        batch.dispose();
        player_img.dispose();
        enemy_img.dispose();
        bullet_img.dispose();
        fontScore.dispose();
        backgroundTexture.dispose();
        for (Texture tex : powerTextures.values()) {
            tex.dispose();
        }
    }
}
