package config

import (
	"encoding/json"
	"os"

	"github.com/HBulgat/migration-sdk-go"
)

type AppConfig struct {
	Server    ServerConfig     `json:"server"`
	Migration migration.Config `json:"migration"`
}

type ServerConfig struct {
	Port int `json:"port"`
}

var Config AppConfig

func init() {
	conf, err := load("Config/Config.json")
	if err != nil {
		panic(err)
	}
	Config = *conf
}

// Load 从指定路径加载配置文件
func load(path string) (*AppConfig, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	var appConfig AppConfig
	if err = json.Unmarshal(data, &appConfig); err != nil {
		return nil, err
	}

	return &appConfig, nil
}
