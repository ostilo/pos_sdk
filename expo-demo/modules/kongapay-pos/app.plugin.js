const fs = require('fs');
const path = require('path');
const {
  withAppBuildGradle,
  withMainApplication,
  withSettingsGradle,
  withDangerousMod,
  createRunOncePlugin,
} = require('@expo/config-plugins');

const PACKAGE_NAME = 'kongapay-pos';
const AF_AAR = 'AFSDKInterface_202502211810_V0.0.236_236.aar';
const POS_AAR = 'pos_utils-release.aar';

function getModuleLibsDir() {
  return path.join(__dirname, 'android', 'libs');
}

/**
 * Copy partner AARs into android/app/libs during prebuild.
 */
function withCopiedAars(config) {
  return withDangerousMod(config, [
    'android',
    async (cfg) => {
      const projectRoot = cfg.modRequest.platformProjectRoot;
      const destDir = path.join(projectRoot, 'app', 'libs');
      fs.mkdirSync(destDir, { recursive: true });

      const srcDir = getModuleLibsDir();
      for (const file of [POS_AAR, AF_AAR]) {
        const from = path.join(srcDir, file);
        const to = path.join(destDir, file);
        if (!fs.existsSync(from)) {
          throw new Error(`KongaPay POS AAR missing: ${from}`);
        }
        fs.copyFileSync(from, to);
      }
      return cfg;
    },
  ]);
}

function withJitPack(config) {
  return withSettingsGradle(config, (cfg) => {
    let contents = cfg.modResults.contents;
    if (!contents.includes('jitpack.io')) {
      if (contents.includes('mavenCentral()')) {
        contents = contents.replace(
          /mavenCentral\(\)/,
          `mavenCentral()\n        maven { url 'https://jitpack.io' }`
        );
      } else if (contents.includes('repositories {')) {
        contents = contents.replace(
          /repositories\s*\{/,
          `repositories {\n        maven { url 'https://jitpack.io' }`
        );
      }
    }
    cfg.modResults.contents = contents;
    return cfg;
  });
}

/**
 * MainApplication must extend ApplicationClass so the POS SDK initializes correctly.
 */
function withKongaPayApplication(config) {
  return withMainApplication(config, (cfg) => {
    let contents = cfg.modResults.contents;

    if (!contents.includes('com.konga.pos_utils.sdk.ApplicationClass')) {
      contents = contents.replace(
        /(package\s+[^\n]+\n)/,
        `$1\nimport com.konga.pos_utils.sdk.ApplicationClass\n`
      );
    }

    if (/class\s+MainApplication\s*:\s*Application\s*\(/.test(contents)) {
      contents = contents.replace(
        /class\s+MainApplication\s*:\s*Application\s*\(/,
        'class MainApplication : ApplicationClass('
      );
    }

    if (/class\s+MainApplication\s+extends\s+Application\b/.test(contents)) {
      contents = contents.replace(
        /class\s+MainApplication\s+extends\s+Application\b/,
        'class MainApplication extends ApplicationClass'
      );
    }

    cfg.modResults.contents = contents;
    return cfg;
  });
}

function withMinSdk29(config) {
  return withAppBuildGradle(config, (cfg) => {
    let contents = cfg.modResults.contents;
    // Expo often uses minSdkVersion rootProject.ext / Integer.parseInt etc.
    if (/minSdkVersion\s+\d+/.test(contents)) {
      contents = contents.replace(/minSdkVersion\s+\d+/, 'minSdkVersion 29');
    } else if (/minSdk\s+\d+/.test(contents)) {
      contents = contents.replace(/minSdk\s+\d+/, 'minSdk 29');
    }
    cfg.modResults.contents = contents;
    return cfg;
  });
}

/**
 * App-level AAR + transitive deps so SDK classes are packaged into the APK.
 */
function withKongaPayAppGradle(config) {
  return withAppBuildGradle(config, (cfg) => {
    let contents = cfg.modResults.contents;
    const marker = '// KongaPay POS SDK deps';
    if (contents.includes(marker)) {
      return cfg;
    }

    const depsBlock = `
    ${marker}
    implementation files("libs/${POS_AAR}")
    implementation files("libs/${AF_AAR}")
    implementation "com.google.code.gson:gson:2.9.0"
    implementation "com.airbnb.android:lottie:6.0.0"
    implementation "com.github.getActivity:TitleBar:9.2"
    implementation "com.github.getActivity:ToastUtils:9.5"
    implementation("com.squareup.retrofit2:retrofit:2.4.0") { exclude group: "com.squareup.okhttp3" }
    implementation("com.squareup.retrofit2:converter-gson:2.4.0") { exclude group: "com.squareup.okhttp3" }
    implementation "com.squareup.okhttp3:okhttp:3.14.9"
    implementation "com.squareup.okhttp3:logging-interceptor:3.14.9"
    implementation "com.squareup.retrofit2:converter-scalars:2.0.0"
`;

    if (contents.includes('dependencies {')) {
      contents = contents.replace(/dependencies\s*\{/, `dependencies {\n${depsBlock}`);
    } else {
      contents += `\ndependencies {\n${depsBlock}\n}\n`;
    }

    cfg.modResults.contents = contents;
    return cfg;
  });
}

function withKongaPayPos(config) {
  config = {
    ...config,
    android: {
      ...config.android,
      minSdkVersion: Math.max(29, config.android?.minSdkVersion ?? 29),
    },
  };
  config = withCopiedAars(config);
  config = withJitPack(config);
  config = withKongaPayApplication(config);
  config = withMinSdk29(config);
  config = withKongaPayAppGradle(config);
  return config;
}

module.exports = createRunOncePlugin(withKongaPayPos, PACKAGE_NAME, '1.0.0');
