他是`Modern Tree View (MTV)`的R和Python版本，它可以通过脚本化的方式进行交互式可视化。他要结合R语言方便的脚本化功能和Java GUI 很强的交互式功能。



## 技术路线
暂定，直接 rJava，和 eGPS2的MTV，直接在R中就写脚本，然后直接出来GUI。



## R包使用的过程


初始化，直接



```r
library(yytree)
# 一定要有这个配置过程，JDK都在这个里面
config_yytree(path2egps = '/your/path/to/egps2')
# 我都电脑就是
config_yytree(path2egps = 'C:\\Users\\yudal\\Documents\\project\\eGPS2\\eGPS_v2_windows_x64_selfTest')
```

不过要注意，rJava需要的是JDK还是JRE，如果是JRE的化要报错。



还可以声明AVA虚拟机的一些配置，比如常用的内存大小



```r
library(yytree)
# 一定要有这个配置过程，JDK都在这个里面
config_yytree(path2egps = '/your/path/to/egps2', max_memory = '10g')

```

