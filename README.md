# NumberAnimationView

一个用于展示数字动画效果的 Android 自定义 View 组件。

## 功能特点

- 支持数字平滑过渡动画效果
- 可自定义动画持续时间
- 可自定义字体大小和颜色
- 支持整数和小数展示

## 使用方法

### XML 布局中使用

```xml
<com.yizhaogeng.numberanimation.NumberAnimationView
    android:id="@+id/number_animation_view"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="24sp"
    android:textColor="@color/black"/>
```

### 代码中设置

```kotlin
numberAnimationView.setNumber(1000.0f) // 设置目标数字
numberAnimationView.setDuration(1000)   // 设置动画持续时间（毫秒）
```

## 安装

1. 在项目级 build.gradle 中添加：
```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

2. 在应用级 build.gradle 中添加依赖：
```gradle
dependencies {
    implementation 'com.github.yizhaogeng:NumberAnimationView:1.0.0'
}
```

## License

本项目基于 MIT 协议开源，详细信息请参阅 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 作者

[@yizhaogeng](https://github.com/yizhaogeng)